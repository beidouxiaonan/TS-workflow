package com.zhixing.ticket.service;

import com.zhixing.ticket.controller.TicketController;
import com.zhixing.ticket.model.Ticket;
import com.zhixing.ticket.model.ReviewRequest;
import com.zhixing.ticket.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkflowLabelsTest {
    @Test void workflowUsesRequestedLabels() {
        TicketService service = new TicketService(mock(TicketRepository.class));
        assertArrayEquals(new String[]{"填写申请","领导审批","目标知识库管理员审批","知识上传","验收审批","完成"},
            (String[]) new TicketController(service).workflow().get("nodes"));
    }

    @Test void legacyLeaderCanContinueThroughEntireWorkflow() {
        TicketRepository repository = mock(TicketRepository.class);
        TicketService service = new TicketService(repository);
        Ticket ticket = new Ticket();
        ticket.setId("test");
        ticket.setCurrentNode("知识库拥有者");
        when(repository.findById("test")).thenReturn(ticket);
        ReviewRequest review = new ReviewRequest();
        assertEquals("领导审批", service.get("test").getCurrentNode());
        assertEquals("目标知识库管理员审批", service.approve("test", review).getCurrentNode());
        assertEquals("知识上传", service.approve("test", review).getCurrentNode());
        assertEquals("验收审批", service.completeTask("test", review).getCurrentNode());
        assertEquals("完成", service.approve("test", review).getCurrentNode());
        assertEquals("已完成", ticket.getStatus());
    }

    @Test void legacyTaskCanSubmitAcceptance() {
        TicketRepository repository = mock(TicketRepository.class);
        TicketService service = new TicketService(repository);
        Ticket ticket = new Ticket();
        ticket.setId("test");
        ticket.setCurrentNode("任务处理");
        when(repository.findById("test")).thenReturn(ticket);
        assertEquals("验收审批", service.completeTask("test", new ReviewRequest()).getCurrentNode());
        assertEquals("待验收", ticket.getStatus());
    }
}
