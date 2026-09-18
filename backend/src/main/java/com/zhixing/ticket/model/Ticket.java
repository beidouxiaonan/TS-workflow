package com.zhixing.ticket.model;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import javax.validation.constraints.Pattern;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Date;
import java.util.List;

public class Ticket {
    @com.fasterxml.jackson.annotation.JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss.SSSXXX",timezone="Asia/Shanghai",lenient=com.fasterxml.jackson.annotation.OptBoolean.FALSE)
    @Schema(description="期望完成时间，选填；PUT省略或null表示清空，包含时区偏移",type="string",format="date-time",example="2026-09-30T18:00:00.000+08:00")
    private Date expectedCompletionTime;
    public Date getExpectedCompletionTime(){return expectedCompletionTime;}
    public void setExpectedCompletionTime(Date value){expectedCompletionTime=value;}
    @NotBlank @Size(max=64) @Schema(description="创建工单时指定的部门领导用户ID",required=true,maxLength=64) private String leaderId;
    @NotBlank @Size(max=64) @Schema(description="创建工单时指定的知识库拥有者用户ID，负责目标知识库管理员审批",required=true,maxLength=64) private String knowledgeBaseOwnerId;
    public String getLeaderId(){return leaderId;}
    public void setLeaderId(String value){leaderId=value;}
    public String getKnowledgeBaseOwnerId(){return knowledgeBaseOwnerId;}
    public void setKnowledgeBaseOwnerId(String value){knowledgeBaseOwnerId=value;}
    @JsonProperty(access=JsonProperty.Access.READ_ONLY) @Schema(accessMode=Schema.AccessMode.READ_ONLY,description="服务端生成的工单编号",example="KB-20260915103000-1001") private String id;
    @NotBlank @Size(max=200) @Schema(description="工单标题",required=true,maxLength=200) private String title;
    @NotBlank @Size(max=64) @Schema(description="申请人姓名或唯一标识",required=true,maxLength=64) private String applicant;
    @Size(max=128) @Schema(description="申请人所属部门",maxLength=128) private String department;
    @Size(max=128) @Schema(description="申请人选择的团队",maxLength=128) private String team;
    @Size(max=200) @Schema(description="目标知识库名称或标识",maxLength=200) private String knowledgeBase;
    @Size(max=64) @Schema(description="文件类型",maxLength=64,example="DOCX / PDF") private String fileType;
    @Size(max=64) @Schema(description="补充类型",maxLength=64,example="操作指引") private String supplementType;
    @Size(max=16000) @Schema(type="string",description="附件ID字符串；多个ID可使用逗号分隔，传空字符串表示清空附件",maxLength=16000,example="ATT-001,ATT-002") private String attachmentIds;
    @NotBlank @Size(max=16000) @Schema(description="知识工单申请原因和具体内容",required=true,maxLength=16000) private String reason;
    @Size(max=16000) @Schema(description="处理或实施方案",maxLength=16000) private String solution;
    @Size(max=16) @Pattern(regexp="普通|高|紧急") @Schema(description="优先级",allowableValues={"普通","高","紧急"},defaultValue="普通") private String priority = "普通";
    @JsonProperty(access=JsonProperty.Access.READ_ONLY) @Schema(accessMode=Schema.AccessMode.READ_ONLY,description="工单状态") private String status = "草稿";
    @JsonProperty(access=JsonProperty.Access.READ_ONLY) @Schema(accessMode=Schema.AccessMode.READ_ONLY,description="当前流程节点") private String currentNode = "填写申请";
    @JsonProperty(access=JsonProperty.Access.READ_ONLY) @Schema(accessMode=Schema.AccessMode.READ_ONLY,description="当前处理人") private String owner = "待指派";
    @JsonProperty(access=JsonProperty.Access.READ_ONLY) @Schema(accessMode=Schema.AccessMode.READ_ONLY,description="创建时间") private Date createdAt;
    @JsonProperty(access=JsonProperty.Access.READ_ONLY) @Schema(accessMode=Schema.AccessMode.READ_ONLY,description="最后更新时间") private Date updatedAt;
    @JsonProperty(access=JsonProperty.Access.READ_ONLY) @Schema(accessMode=Schema.AccessMode.READ_ONLY,description="审批和处理记录") private List<ProcessRecord> process = new java.util.ArrayList<ProcessRecord>();

    public String getId(){return id;} public void setId(String id){this.id=id;}
    public String getTitle(){return title;} public void setTitle(String title){this.title=title;}
    public String getApplicant(){return applicant;} public void setApplicant(String applicant){this.applicant=applicant;}
    public String getDepartment(){return department;} public void setDepartment(String department){this.department=department;}
    public String getTeam(){return team;} public void setTeam(String team){this.team=team;}
    public String getKnowledgeBase(){return knowledgeBase;} public void setKnowledgeBase(String knowledgeBase){this.knowledgeBase=knowledgeBase;}
    public String getFileType(){return fileType;} public void setFileType(String fileType){this.fileType=fileType;}
    public String getSupplementType(){return supplementType;} public void setSupplementType(String supplementType){this.supplementType=supplementType;}
    public String getAttachmentIds(){return attachmentIds;} public void setAttachmentIds(String attachmentIds){this.attachmentIds=attachmentIds;}
    public String getReason(){return reason;} public void setReason(String reason){this.reason=reason;}
    public String getSolution(){return solution;} public void setSolution(String solution){this.solution=solution;}
    public String getPriority(){return priority;} public void setPriority(String priority){this.priority=priority;}
    public String getStatus(){return status;} public void setStatus(String status){this.status=status;}
    public String getCurrentNode(){return currentNode;} public void setCurrentNode(String currentNode){this.currentNode=currentNode;}
    public String getOwner(){return owner;} public void setOwner(String owner){this.owner=owner;}
    public Date getCreatedAt(){return createdAt;} public void setCreatedAt(Date createdAt){this.createdAt=createdAt;}
    public Date getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Date updatedAt){this.updatedAt=updatedAt;}
    public List<ProcessRecord> getProcess(){return process;} public void setProcess(List<ProcessRecord> process){this.process=process;}
}
