package com.zhixing.ticket.service;
import com.zhixing.ticket.model.*;
import com.zhixing.ticket.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WithdrawDraftTest {
    TicketRepository repository=mock(TicketRepository.class);
    TicketService service=new TicketService(repository);
    Ticket ticket=new Ticket();
    WithdrawRequest request=new WithdrawRequest();
    WithdrawDraftTest(){ticket.setId("test");ticket.setApplicant("USER-001");ticket.setAttachmentIds("ATT-1");ticket.setLeaderId("LEADER");request.setApplicant("USER-001");when(repository.findById("test")).thenReturn(ticket);}
    @Test void withdrawsBothApprovalNodesAndKeepsData(){
        for(String node:new String[]{"领导审批","目标知识库管理员审批"}){
            ticket.setStatus("审批中");ticket.setCurrentNode(node);
            service.withdraw("test",request);
            assertEquals("草稿",ticket.getStatus());assertEquals("填写申请",ticket.getCurrentNode());assertEquals("待指派",ticket.getOwner());assertEquals("ATT-1",ticket.getAttachmentIds());assertEquals("LEADER",ticket.getLeaderId());
        }
        verify(repository,times(2)).addProcess(eq("test"),argThat(p->"撤销申请".equals(p.getAction())));
    }
    @Test void rejectsOtherApplicant(){request.setApplicant("OTHER");assertThrows(SecurityException.class,()->service.withdraw("test",request));assertThrows(SecurityException.class,()->service.deleteDraft("test","OTHER"));verify(repository,never()).update(any());verify(repository,never()).deleteDraft(anyString());}
    @Test void rejectsInvalidStates(){for(String status:new String[]{"草稿","处理中","待验收","已完成","已退回"}){ticket.setStatus(status);assertThrows(IllegalStateException.class,()->service.withdraw("test",request));}ticket.setStatus("审批中");ticket.setCurrentNode("知识上传");assertThrows(IllegalStateException.class,()->service.withdraw("test",request));assertThrows(IllegalStateException.class,()->service.deleteDraft("test","USER-001"));}
    @Test void deletesOnlyDraft(){service.deleteDraft("test","USER-001");verify(repository).lock("test");verify(repository).deleteDraft("test");}
    @Test void missingTicketDoesNotDelete(){when(repository.findById("test")).thenReturn(null);assertThrows(IllegalArgumentException.class,()->service.deleteDraft("test","USER-001"));verify(repository,never()).deleteDraft(anyString());}
}
