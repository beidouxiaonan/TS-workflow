package com.zhixing.ticket.model;

import java.util.Date;
import io.swagger.v3.oas.annotations.media.Schema;

public class ProcessRecord {
    @Schema(description="发生操作时所在的流程节点",maxLength=64) private String node;
    @Schema(description="操作人",maxLength=64) private String operator;
    @Schema(description="操作名称",maxLength=32) private String action;
    @Schema(description="操作或审批意见",maxLength=1000) private String comment;
    @Schema(description="操作时间") private Date operatedAt;

    public ProcessRecord() {}
    public ProcessRecord(String node, String operator, String action, String comment, Date operatedAt) {
        this.node=node; this.operator=operator; this.action=action; this.comment=comment; this.operatedAt=operatedAt;
    }
    public String getNode(){return node;} public void setNode(String node){this.node=node;}
    public String getOperator(){return operator;} public void setOperator(String operator){this.operator=operator;}
    public String getAction(){return action;} public void setAction(String action){this.action=action;}
    public String getComment(){return comment;} public void setComment(String comment){this.comment=comment;}
    public Date getOperatedAt(){return operatedAt;} public void setOperatedAt(Date operatedAt){this.operatedAt=operatedAt;}
}
