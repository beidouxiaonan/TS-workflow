package com.zhixing.ticket.service;

import com.zhixing.ticket.controller.TicketController;
import com.zhixing.ticket.repository.TicketRepository;
import com.zhixing.ticket.model.Ticket;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import static org.mockito.Mockito.*;

class TicketNodeFilterTest {
    @Test void forwardsNodeAndOwner() {
        TicketRepository repository=mock(TicketRepository.class);
        when(repository.findPage(null,null,"USER-001","目标知识库管理员审批",null,1,20)).thenReturn(new com.zhixing.ticket.model.TicketPage(java.util.Collections.emptyList(),0,1,20));
        new TicketController(new TicketService(repository)).list(null,null,"USER-001","目标知识库管理员审批",null,1,20);
        verify(repository).findPage(null,null,"USER-001","目标知识库管理员审批",null,1,20);
    }
    @Test void filtersAdministratorByExactNode() {
        JdbcTemplate jdbc=mock(JdbcTemplate.class);
        new TicketRepository(jdbc).findPage(null,null,"USER-001","目标知识库管理员审批",null,1,20);
        verify(jdbc).query(eq("SELECT * FROM kb_ticket WHERE 1=1 AND status<>'草稿' AND owner=? AND current_node=? ORDER BY CASE priority WHEN '紧急' THEN 0 WHEN '高' THEN 1 WHEN '普通' THEN 2 ELSE 3 END,created_at DESC,id DESC LIMIT ? OFFSET ?"),
            org.mockito.ArgumentMatchers.<RowMapper<Ticket>>any(),eq("USER-001"),eq("目标知识库管理员审批"),eq(20),eq(0L));
    }
    @Test void leaderAndUploadIncludeLegacyLabels() {
        String[][] pairs={{"领导审批","知识库拥有者"},{"知识上传","任务处理"}};
        for(String[] pair:pairs){
            JdbcTemplate jdbc=mock(JdbcTemplate.class);
            new TicketRepository(jdbc).findPage(null,null,"USER-001",pair[0],null,1,20);
            verify(jdbc).query(eq("SELECT * FROM kb_ticket WHERE 1=1 AND status<>'草稿' AND owner=? AND current_node IN (?,?) ORDER BY CASE priority WHEN '紧急' THEN 0 WHEN '高' THEN 1 WHEN '普通' THEN 2 ELSE 3 END,created_at DESC,id DESC LIMIT ? OFFSET ?"),
                org.mockito.ArgumentMatchers.<RowMapper<Ticket>>any(),eq("USER-001"),eq(pair[0]),eq(pair[1]),eq(20),eq(0L));
        }
    }
}
