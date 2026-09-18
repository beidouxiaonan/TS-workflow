package com.zhixing.ticket.service;

import com.zhixing.ticket.controller.TicketController;
import com.zhixing.ticket.repository.TicketRepository;
import com.zhixing.ticket.model.Ticket;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import java.util.Collections;
import static org.mockito.Mockito.*;

class TicketOwnerFilterTest {
    @Test void forwardsOwnerFromController() {
        TicketRepository repository=mock(TicketRepository.class);
        when(repository.findPage(null,null,"USER-001",null,null,1,20)).thenReturn(new com.zhixing.ticket.model.TicketPage(Collections.emptyList(),0,1,20));
        new TicketController(new TicketService(repository)).list(null,null,"USER-001",null,null,1,20);
        verify(repository).findPage(null,null,"USER-001",null,null,1,20);
    }

    @Test void combinesFiltersWithBoundParameters() {
        JdbcTemplate jdbc=mock(JdbcTemplate.class);
        new TicketRepository(jdbc).findPage("审批中","USER-002","USER-001",null,null,1,20);
        verify(jdbc).query(eq("SELECT * FROM kb_ticket WHERE 1=1 AND status=? AND applicant=? AND owner=? ORDER BY CASE priority WHEN '紧急' THEN 0 WHEN '高' THEN 1 WHEN '普通' THEN 2 ELSE 3 END,created_at DESC,id DESC LIMIT ? OFFSET ?"),
            org.mockito.ArgumentMatchers.<RowMapper<Ticket>>any(),eq("审批中"),eq("USER-002"),eq("USER-001"),eq(20),eq(0L));
    }

    @Test void omittedOwnerKeepsExistingQuery() {
        JdbcTemplate jdbc=mock(JdbcTemplate.class);
        new TicketRepository(jdbc).findPage(null,null,null,null,null,1,20);
        verify(jdbc).query(eq("SELECT * FROM kb_ticket WHERE 1=1 AND status<>'草稿' ORDER BY CASE priority WHEN '紧急' THEN 0 WHEN '高' THEN 1 WHEN '普通' THEN 2 ELSE 3 END,created_at DESC,id DESC LIMIT ? OFFSET ?"),
            org.mockito.ArgumentMatchers.<RowMapper<Ticket>>any(),eq(20),eq(0L));
    }
}
