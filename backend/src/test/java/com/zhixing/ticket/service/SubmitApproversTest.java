package com.zhixing.ticket.service;

import com.zhixing.ticket.model.*;
import com.zhixing.ticket.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SubmitApproversTest {
    @Test void creationAcceptsApproversAndSubmitHasNoBody() throws Exception {
        Ticket ticket=new com.fasterxml.jackson.databind.ObjectMapper().readValue(
            "{\"leaderId\":\"USER-001\",\"knowledgeBaseOwnerId\":\"USER-002\",\"attachmentIds\":\"ATT-1\"}",Ticket.class);
        assertEquals("USER-001",ticket.getLeaderId());
        assertEquals("USER-002",ticket.getKnowledgeBaseOwnerId());
        TicketRepository repository=mock(TicketRepository.class);
        when(repository.findById(anyString())).thenReturn(ticket);
        new TicketService(repository).create(ticket);
        verify(repository).insert(ticket);
        assertEquals(1,com.zhixing.ticket.controller.TicketController.class.getMethod("submit",String.class).getParameterCount());
    }
    @Test void assignsAndPreservesApprovers() {
        TicketRepository repository=mock(TicketRepository.class);
        TicketService service=new TicketService(repository);
        Ticket ticket=new Ticket(); ticket.setId("test");
        when(repository.findById("test")).thenReturn(ticket);
        Ticket request=ticket;
        request.setLeaderId("USER-001"); request.setKnowledgeBaseOwnerId("USER-002");
        request.setAttachmentIds("ATT-1,ATT-2");
        Ticket result=service.submit("test");
        assertEquals("USER-001",result.getOwner());
        assertEquals("USER-002",result.getKnowledgeBaseOwnerId());
        assertEquals("ATT-1,ATT-2",result.getAttachmentIds());
        verify(repository).update(ticket);
        service.approve("test",new ReviewRequest());
        assertEquals("目标知识库管理员审批",ticket.getCurrentNode());
        assertEquals("USER-002",ticket.getOwner());
        Ticket update=new Ticket();
        service.update("test",update);
        assertEquals("USER-001",update.getLeaderId());
        assertEquals("USER-002",update.getKnowledgeBaseOwnerId());
    }

    @Test void rejectsMissingApproversBeforeWriting() {
        TicketRepository repository=mock(TicketRepository.class);
        TicketService service=new TicketService(repository);
        when(repository.findById("test")).thenReturn(new Ticket());
        assertThrows(IllegalArgumentException.class,()->service.submit("test"));
        verify(repository,never()).update(any());
    }

    @Test void validatesRequiredAndLengthConstraints() {
        try(ValidatorFactory factory=Validation.buildDefaultValidatorFactory()) {
            Ticket request=new Ticket();
            request.setTitle("测试"); request.setApplicant("USER-001"); request.setReason("测试原因");
            assertEquals(2,factory.getValidator().validate(request).size());
            request.setLeaderId("USER-001"); request.setKnowledgeBaseOwnerId("USER-002");
            assertTrue(factory.getValidator().validate(request).isEmpty());
            request.setLeaderId(new String(new char[65]).replace('\0','a'));
            assertFalse(factory.getValidator().validate(request).isEmpty());
            request.setLeaderId("   ");
            assertFalse(factory.getValidator().validate(request).isEmpty());
        }
    }
}
