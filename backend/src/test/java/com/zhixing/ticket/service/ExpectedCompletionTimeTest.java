package com.zhixing.ticket.service;

import com.zhixing.ticket.model.Ticket;
import com.zhixing.ticket.repository.TicketRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import java.sql.Timestamp;
import java.sql.ResultSet;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExpectedCompletionTimeTest {
    @Test void jsonRoundTripAndInvalidDate() throws Exception {
        ObjectMapper mapper=new ObjectMapper();
        Ticket ticket=mapper.readValue("{\"expectedCompletionTime\":\"2026-09-30T18:00:00.000+08:00\"}",Ticket.class);
        assertEquals("2026-09-30T18:00:00.000+08:00",mapper.valueToTree(ticket).get("expectedCompletionTime").asText());
        assertNull(mapper.readValue("{}",Ticket.class).getExpectedCompletionTime());
        assertNull(mapper.readValue("{\"expectedCompletionTime\":null}",Ticket.class).getExpectedCompletionTime());
        assertThrows(Exception.class,()->mapper.readValue("{\"expectedCompletionTime\":\"not-a-date\"}",Ticket.class));
    }
    @Test void writesTimestampAndNull() {
        for(Date value:new Date[]{new Date(1790762400000L),null}){
            JdbcTemplate jdbc=mock(JdbcTemplate.class);TicketRepository repository=new TicketRepository(jdbc);
            Ticket ticket=new Ticket();ticket.setId("test");ticket.setCreatedAt(new Date());ticket.setUpdatedAt(new Date());ticket.setExpectedCompletionTime(value);
            repository.insert(ticket);repository.update(ticket);
            for(org.mockito.invocation.Invocation call:mockingDetails(jdbc).getInvocations()){
                Object[] raw=call.getRawArguments();String sql=(String)raw[0];Object[] args=(Object[])raw[1];
                assertTrue(sql.contains("expected_completion_time"));
                Object actual=args[sql.startsWith("INSERT")?args.length-1:args.length-2];
                assertEquals(value==null?null:new Timestamp(value.getTime()),actual);
            }
        }
    }
    @Test void readsTimestampFromDatabase() throws Exception {
        JdbcTemplate jdbc=mock(JdbcTemplate.class);new TicketRepository(jdbc).findById("test");
        RowMapper<?> mapper=(RowMapper<?>)mockingDetails(jdbc).getInvocations().iterator().next().getRawArguments()[1];
        ResultSet rs=mock(ResultSet.class);Timestamp value=new Timestamp(1790762400000L);
        when(rs.getTimestamp("expected_completion_time")).thenReturn(value);
        assertEquals(value,((Ticket)mapper.mapRow(rs,0)).getExpectedCompletionTime());
    }
}
