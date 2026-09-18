package com.zhixing.ticket.model;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

public class WithdrawRequest {
    @NotBlank @Size(max=64) @Schema(description="申请者标识，必须与工单 applicant 一致；当前无登录认证",required=true,maxLength=64)
    private String applicant;
    @Size(max=1000) @Schema(description="撤销原因",maxLength=1000)
    private String comment;
    public String getApplicant(){return applicant;}
    public void setApplicant(String value){applicant=value;}
    public String getComment(){return comment;}
    public void setComment(String value){comment=value;}
}
