package com.zhixing.ticket.service;

import com.zhixing.ticket.model.*;
import com.zhixing.ticket.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TicketPaginationTest {
    @Test void countAndPageUseSameFiltersAndEscapeWildcards(){
        JdbcTemplate jdbc=mock(JdbcTemplate.class);
        String where=" FROM kb_ticket WHERE 1=1 AND status<>'草稿' AND applicant=? AND owner=? AND title LIKE ? ESCAPE '!'";
        when(jdbc.queryForObject(eq("SELECT COUNT(*)"+where),eq(Long.class),eq("A"),eq("B"),eq("%文!%!_!!%"))).thenReturn(25L);
        TicketPage result=new TicketRepository(jdbc).findPage(null,"A","B",null," 文%_! ",2,10);
        verify(jdbc).query(eq("SELECT *"+where+" ORDER BY CASE priority WHEN '紧急' THEN 0 WHEN '高' THEN 1 WHEN '普通' THEN 2 ELSE 3 END,created_at DESC,id DESC LIMIT ? OFFSET ?"),
            org.mockito.ArgumentMatchers.<RowMapper<Ticket>>any(),eq("A"),eq("B"),eq("%文!%!_!!%"),eq(10),eq(10L));
        assertEquals(25,result.getTotal());assertEquals(3,result.getTotalPages());assertEquals(2,result.getPage());assertEquals(10,result.getPageSize());
    }
    @Test void invalidPageRejectedBeforeQuery(){
        TicketRepository repository=mock(TicketRepository.class);TicketService service=new TicketService(repository);
        for(int[] values:new int[][]{{0,20},{-1,20},{1,0},{1,101}})
            assertThrows(IllegalStateException.class,()->service.list(null,null,null,null,null,values[0],values[1]));
        verifyNoInteractions(repository);
    }
    @Test void offsetUsesLongAndOutOfRangePageIsEmpty(){
        JdbcTemplate jdbc=mock(JdbcTemplate.class);
        TicketPage result=new TicketRepository(jdbc).findPage(null,null,null,null,"  ",Integer.MAX_VALUE,100);
        verify(jdbc).query(eq("SELECT * FROM kb_ticket WHERE 1=1 AND status<>'草稿' ORDER BY CASE priority WHEN '紧急' THEN 0 WHEN '高' THEN 1 WHEN '普通' THEN 2 ELSE 3 END,created_at DESC,id DESC LIMIT ? OFFSET ?"),
            org.mockito.ArgumentMatchers.<RowMapper<Ticket>>any(),eq(100),eq(214748364600L));
        assertTrue(result.getItems().isEmpty());assertEquals(0,result.getTotalPages());
    }
    @Test void responseHasPaginationFields() throws Exception {
        com.fasterxml.jackson.databind.JsonNode json=new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(new TicketPage(Collections.emptyList(),0,1,20));
        assertTrue(json.get("items").isArray());assertEquals(0,json.get("total").asLong());assertEquals(1,json.get("page").asInt());assertEquals(20,json.get("pageSize").asInt());assertEquals(0,json.get("totalPages").asLong());
    }
}
