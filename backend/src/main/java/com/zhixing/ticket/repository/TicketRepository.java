package com.zhixing.ticket.repository;

import com.zhixing.ticket.model.ProcessRecord;
import com.zhixing.ticket.model.Ticket;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Repository
public class TicketRepository {
    private final JdbcTemplate jdbc;
    public TicketRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
    // All mutations acquire the same row lock in their service transaction.
    public void lock(String id){jdbc.queryForList("SELECT id FROM kb_ticket WHERE id=? FOR UPDATE",id);}
    public void deleteDraft(String id){
        jdbc.update("DELETE FROM kb_ticket_process WHERE ticket_id=?",id);
        jdbc.update("DELETE FROM kb_ticket WHERE id=?",id);
    }

    private final RowMapper<Ticket> ticketMapper=new RowMapper<Ticket>(){
        public Ticket mapRow(ResultSet rs,int rowNum)throws SQLException{
            Ticket t=new Ticket();t.setLeaderId(rs.getString("leader_id"));t.setKnowledgeBaseOwnerId(rs.getString("knowledge_base_owner_id"));t.setId(rs.getString("id"));t.setTitle(rs.getString("title"));t.setApplicant(rs.getString("applicant"));
            t.setDepartment(rs.getString("department"));t.setTeam(rs.getString("team"));t.setKnowledgeBase(rs.getString("knowledge_base"));
            t.setFileType(rs.getString("file_type"));t.setSupplementType(rs.getString("supplement_type"));t.setAttachmentIds(rs.getString("attachment_ids"));t.setReason(rs.getString("reason"));
            t.setSolution(rs.getString("solution"));t.setPriority(rs.getString("priority"));t.setStatus(rs.getString("status"));
            t.setCurrentNode(rs.getString("current_node"));t.setOwner(rs.getString("owner"));
            t.setExpectedCompletionTime(rs.getTimestamp("expected_completion_time"));t.setCreatedAt(rs.getTimestamp("created_at"));t.setUpdatedAt(rs.getTimestamp("updated_at"));return t;
        }};

    public com.zhixing.ticket.model.TicketPage findPage(String status,String applicant,String owner,String currentNode,String title,int page,int pageSize){StringBuilder sql=new StringBuilder(" FROM kb_ticket WHERE 1=1");List<Object> args=new ArrayList<Object>();if(status!=null&&!status.trim().isEmpty()){sql.append(" AND status=?");args.add(status.trim());}else{sql.append(" AND status<>\'草稿\'");}if(applicant!=null&&!applicant.isEmpty()){sql.append(" AND applicant=?");args.add(applicant);}if(owner!=null&&!owner.isEmpty()){sql.append(" AND owner=?");args.add(owner);}if(currentNode!=null&&!currentNode.isEmpty()){
        // Match historical labels as well as the current workflow labels.
        if("领导审批".equals(currentNode)||"知识上传".equals(currentNode)){
            sql.append(" AND current_node IN (?,?)");args.add(currentNode);
            args.add("领导审批".equals(currentNode)?"知识库拥有者":"任务处理");
        }else{sql.append(" AND current_node=?");args.add(currentNode);}
    }if(title!=null&&!title.trim().isEmpty()){
        sql.append(" AND title LIKE ? ESCAPE '!'");
        args.add("%"+title.trim().replace("!","!!").replace("%","!%").replace("_","!_")+"%");
    }
    Long count=jdbc.queryForObject("SELECT COUNT(*)"+sql.toString(),Long.class,args.toArray());
    args.add(pageSize);args.add(((long)page-1)*pageSize);
    List<Ticket> list=jdbc.query("SELECT *"+sql.toString()+" ORDER BY CASE priority WHEN '紧急' THEN 0 WHEN '高' THEN 1 WHEN '普通' THEN 2 ELSE 3 END,created_at DESC,id DESC LIMIT ? OFFSET ?",ticketMapper,args.toArray());
    for(Ticket t:list)t.setProcess(findProcess(t.getId()));
    return new com.zhixing.ticket.model.TicketPage(list,count==null?0:count,page,pageSize);
    }
    public Ticket findById(String id){List<Ticket> list=jdbc.query("SELECT * FROM kb_ticket WHERE id=?",ticketMapper,id);if(list.isEmpty())return null;Ticket t=list.get(0);t.setProcess(findProcess(id));return t;}
    public void insert(Ticket t){jdbc.update("INSERT INTO kb_ticket(id,title,applicant,department,team,knowledge_base,file_type,supplement_type,attachment_ids,reason,solution,priority,status,current_node,owner,created_at,updated_at,leader_id,knowledge_base_owner_id,expected_completion_time) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",t.getId(),t.getTitle(),t.getApplicant(),t.getDepartment(),t.getTeam(),t.getKnowledgeBase(),t.getFileType(),t.getSupplementType(),t.getAttachmentIds(),t.getReason(),t.getSolution(),t.getPriority(),t.getStatus(),t.getCurrentNode(),t.getOwner(),new Timestamp(t.getCreatedAt().getTime()),new Timestamp(t.getUpdatedAt().getTime()),t.getLeaderId(),t.getKnowledgeBaseOwnerId(),t.getExpectedCompletionTime()==null?null:new Timestamp(t.getExpectedCompletionTime().getTime()));}
    public void update(Ticket t){jdbc.update("UPDATE kb_ticket SET title=?,applicant=?,department=?,team=?,knowledge_base=?,file_type=?,supplement_type=?,attachment_ids=?,reason=?,solution=?,priority=?,status=?,current_node=?,owner=?,updated_at=?,leader_id=?,knowledge_base_owner_id=?,expected_completion_time=? WHERE id=?",t.getTitle(),t.getApplicant(),t.getDepartment(),t.getTeam(),t.getKnowledgeBase(),t.getFileType(),t.getSupplementType(),t.getAttachmentIds(),t.getReason(),t.getSolution(),t.getPriority(),t.getStatus(),t.getCurrentNode(),t.getOwner(),new Timestamp(t.getUpdatedAt().getTime()),t.getLeaderId(),t.getKnowledgeBaseOwnerId(),t.getExpectedCompletionTime()==null?null:new Timestamp(t.getExpectedCompletionTime().getTime()),t.getId());}
    public void addProcess(String ticketId,ProcessRecord p){jdbc.update("INSERT INTO kb_ticket_process(ticket_id,node_name,operator_name,action_name,comment_text,operated_at) VALUES(?,?,?,?,?,?)",ticketId,p.getNode(),p.getOperator(),p.getAction(),p.getComment(),new Timestamp(p.getOperatedAt().getTime()));}
    public List<ProcessRecord> findProcess(String ticketId){return jdbc.query("SELECT node_name,operator_name,action_name,comment_text,operated_at FROM kb_ticket_process WHERE ticket_id=? ORDER BY operated_at,process_id",(rs,n)->new ProcessRecord(rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getTimestamp(5)),ticketId);}
}
