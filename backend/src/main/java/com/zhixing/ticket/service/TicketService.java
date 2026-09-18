package com.zhixing.ticket.service;

import com.zhixing.ticket.model.ProcessRecord;
import com.zhixing.ticket.model.ReviewRequest;
import com.zhixing.ticket.model.Ticket;
import com.zhixing.ticket.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class TicketService {
    private final TicketRepository repository;
    private final AtomicInteger sequence=new AtomicInteger(1000);
    public TicketService(TicketRepository repository){this.repository=repository;}
    @Transactional(readOnly=true,isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public com.zhixing.ticket.model.TicketPage list(String status,String applicant,String owner,String currentNode,String title,int page,int pageSize){
        if(page<1||pageSize<1||pageSize>100)throw new IllegalStateException("page须大于等于1，pageSize须在1至100之间");
        com.zhixing.ticket.model.TicketPage result=repository.findPage(status,applicant,owner,normalizeNode(currentNode),title,page,pageSize);
        for(Ticket ticket:result.getItems())normalizeNodes(ticket);
        return result;
    }
    public Ticket get(String id){Ticket t=repository.findById(id);if(t==null)throw new IllegalArgumentException("工单不存在: "+id);return normalizeNodes(t);}
    @Transactional public Ticket create(Ticket t){Date now=new Date();if(t.getId()==null||t.getId().trim().isEmpty())t.setId("KB-"+new SimpleDateFormat("yyyyMMddHHmmss").format(now)+"-"+sequence.incrementAndGet());t.setCreatedAt(now);t.setUpdatedAt(now);if(t.getStatus()==null)t.setStatus("草稿");if(t.getCurrentNode()==null)t.setCurrentNode("填写申请");if(t.getOwner()==null)t.setOwner("待指派");repository.insert(t);add(t,"填写申请",t.getApplicant(),"创建草稿","工单信息已保存",now);return get(t.getId());}
    @Transactional public Ticket update(String id,Ticket input){repository.lock(id);Ticket old=get(id);input.setId(id);input.setCreatedAt(old.getCreatedAt());input.setUpdatedAt(new Date());input.setStatus(old.getStatus());input.setCurrentNode(old.getCurrentNode());input.setOwner(old.getOwner());if(!"填写申请".equals(old.getCurrentNode())){input.setLeaderId(old.getLeaderId());input.setKnowledgeBaseOwnerId(old.getKnowledgeBaseOwnerId());}repository.update(input);return get(id);}
    @Transactional public Ticket submit(String id){repository.lock(id);Ticket t=get(id);if(t.getLeaderId()==null||t.getLeaderId().trim().isEmpty()||t.getKnowledgeBaseOwnerId()==null||t.getKnowledgeBaseOwnerId().trim().isEmpty())throw new IllegalArgumentException("工单缺少审批人，请先更新工单的部门领导和知识库拥有者用户ID");t.setOwner(t.getLeaderId());String node=t.getCurrentNode();t.setStatus("审批中");t.setCurrentNode("领导审批");t.setUpdatedAt(new Date());repository.update(t);add(t,node,t.getApplicant(),"提交申请","进入领导审批",t.getUpdatedAt());return get(id);}
    @Transactional public Ticket approve(String id,ReviewRequest r){repository.lock(id);Ticket t=get(id);String node=t.getCurrentNode();if("领导审批".equals(node)){t.setCurrentNode("目标知识库管理员审批");t.setOwner(t.getKnowledgeBaseOwnerId()==null?"待指派":t.getKnowledgeBaseOwnerId());}else if("目标知识库管理员审批".equals(node)){t.setCurrentNode("知识上传");t.setStatus("处理中");}else if("验收审批".equals(node)){t.setCurrentNode("完成");t.setStatus("已完成");}else throw new IllegalStateException("当前节点不支持审批: "+node);t.setUpdatedAt(new Date());repository.update(t);add(t,node,r.getReviewer(),"审核通过",r.getComment(),t.getUpdatedAt());return get(id);}
    @Transactional public Ticket reject(String id,ReviewRequest r){repository.lock(id);Ticket t=get(id);String node=t.getCurrentNode();t.setStatus("已退回");t.setCurrentNode("填写申请");t.setUpdatedAt(new Date());repository.update(t);add(t,node,r.getReviewer(),"退回修改",r.getComment(),t.getUpdatedAt());return get(id);}
    @Transactional public Ticket completeTask(String id,ReviewRequest r){repository.lock(id);Ticket t=get(id);if(!"知识上传".equals(t.getCurrentNode()))throw new IllegalStateException("当前工单不在知识上传节点");t.setStatus("待验收");t.setCurrentNode("验收审批");t.setUpdatedAt(new Date());repository.update(t);add(t,"知识上传",r.getReviewer(),"提交验收",r.getComment(),t.getUpdatedAt());return get(id);}
    // Translate legacy stored labels without changing workflow states or historical comments.
    @Transactional public Ticket withdraw(String id,com.zhixing.ticket.model.WithdrawRequest request){
        repository.lock(id);
        Ticket ticket=get(id);
        checkApplicant(ticket,request.getApplicant());
        if(!"审批中".equals(ticket.getStatus()) || !("领导审批".equals(ticket.getCurrentNode()) || "目标知识库管理员审批".equals(ticket.getCurrentNode())))
            throw new IllegalStateException("仅待领导或知识库管理员审批的工单可以撤销");
        String node=ticket.getCurrentNode();
        ticket.setStatus("草稿");ticket.setCurrentNode("填写申请");ticket.setOwner("待指派");ticket.setUpdatedAt(new Date());
        repository.update(ticket);
        add(ticket,node,ticket.getApplicant(),"撤销申请",request.getComment(),ticket.getUpdatedAt());
        return get(id);
    }
    @Transactional public void deleteDraft(String id,String applicant){
        repository.lock(id);
        Ticket ticket=get(id);checkApplicant(ticket,applicant);
        if(!"草稿".equals(ticket.getStatus()) || !"填写申请".equals(ticket.getCurrentNode()))throw new IllegalStateException("只能删除草稿工单");
        repository.deleteDraft(id);
    }
    private void checkApplicant(Ticket ticket,String applicant){
        if(applicant==null || applicant.trim().isEmpty() || !applicant.equals(ticket.getApplicant()))throw new SecurityException("仅工单申请者可以执行此操作");
    }
    private String normalizeNode(String node){
        if("知识库拥有者".equals(node))return "领导审批";
        if("任务处理".equals(node))return "知识上传";
        return node;
    }
    private Ticket normalizeNodes(Ticket ticket){
        ticket.setCurrentNode(normalizeNode(ticket.getCurrentNode()));
        if(ticket.getProcess()!=null)for(ProcessRecord record:ticket.getProcess())record.setNode(normalizeNode(record.getNode()));
        return ticket;
    }
    private void add(Ticket t,String node,String operator,String action,String comment,Date time){repository.addProcess(t.getId(),new ProcessRecord(node,operator,action,comment,time));}
}
