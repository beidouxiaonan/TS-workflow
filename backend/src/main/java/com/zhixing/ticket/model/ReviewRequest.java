package com.zhixing.ticket.model;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

public class ReviewRequest {
    @NotBlank @Size(max=64) @Schema(description="审核人姓名或唯一标识",required=true,maxLength=64) private String reviewer;
    @Size(max=1000) @Schema(description="审批意见",maxLength=1000) private String comment;
    public String getReviewer(){return reviewer;} public void setReviewer(String reviewer){this.reviewer=reviewer;}
    public String getComment(){return comment;} public void setComment(String comment){this.comment=comment;}
}
