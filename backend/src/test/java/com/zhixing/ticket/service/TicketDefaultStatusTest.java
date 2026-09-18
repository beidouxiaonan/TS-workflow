package com.zhixing.ticket.service;

import com.zhixing.ticket.model.Ticket;
import com.zhixing.ticket.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import static org.mockito.Mockito.*;

class TicketDefaultStatusTest {
    private final String order=" ORDER BY CASE priority WHEN '紧急' THEN 0 WHEN '高' THEN 1 WHEN '普通' THEN 2 ELSE 3 END,created_at DESC,id DESC LIMIT ? OFFSET ?";
    @Test void absentAndBlankStatusExcludeDraftFromCountAndPage(){
        for(String status:new String[]{null,"","   "}){
            JdbcTemplate jdbc=mock(JdbcTemplate.class);
            new TicketRepository(jdbc).findPage(status,null,null,null,null,1,20);
            verify(jdbc).queryForObject(eq("SELECT COUNT(*) FROM kb_ticket WHERE 1=1 AND status<>'草稿'"),eq(Long.class),new Object[0]);
            verify(jdbc).query(eq("SELECT * FROM kb_ticket WHERE 1=1 AND status<>'草稿'"+order),org.mockito.ArgumentMatchers.<RowMapper<Ticket>>any(),eq(20),eq(0L));
        }
    }
    @Test void explicitDraftRemainsAvailable(){
        JdbcTemplate jdbc=mock(JdbcTemplate.class);
        new TicketRepository(jdbc).findPage("草稿",null,null,null,null,1,20);
        verify(jdbc).queryForObject(eq("SELECT COUNT(*) FROM kb_ticket WHERE 1=1 AND status=?"),eq(Long.class),eq("草稿"));
        verify(jdbc).query(eq("SELECT * FROM kb_ticket WHERE 1=1 AND status=?"+order),org.mockito.ArgumentMatchers.<RowMapper<Ticket>>any(),eq("草稿"),eq(20),eq(0L));
    }
}
