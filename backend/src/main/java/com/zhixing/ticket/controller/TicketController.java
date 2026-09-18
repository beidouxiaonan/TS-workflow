package com.zhixing.ticket.controller;

import com.zhixing.ticket.model.ReviewRequest;
import com.zhixing.ticket.model.Ticket;
import com.zhixing.ticket.service.TicketService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.Size;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tickets")
@CrossOrigin(origins="*")
@Validated
public class TicketController {
    private final TicketService service;
    public TicketController(TicketService service){this.service=service;}

    @io.swagger.v3.oas.annotations.Operation(summary="搜索并分页查询工单",description="我的工单与审核工单共用；title包含匹配，applicant精确匹配，所有条件AND组合；默认排除草稿，按紧急、高、普通排序，同优先级按创建时间和ID倒序")
    @GetMapping public com.zhixing.ticket.model.TicketPage list(@io.swagger.v3.oas.annotations.Parameter(description="状态精确筛选；不传、空字符串或全空白时排除草稿；查询草稿须显式传草稿") @RequestParam(required=false) @Size(max=32) String status,@RequestParam(required=false) @Size(max=64) String applicant,@io.swagger.v3.oas.annotations.Parameter(description="当前处理人用户ID，精确匹配；与其他条件按AND组合") @RequestParam(required=false) @Size(max=64) String owner,@io.swagger.v3.oas.annotations.Parameter(description="当前流程节点；所在部门审批传领导审批，知识库管理员审批传目标知识库管理员审批，知识上传传知识上传。与其他条件按AND组合") @RequestParam(required=false) @Size(max=64) String currentNode,
        @io.swagger.v3.oas.annotations.Parameter(description="标题包含搜索，%和_按普通字符匹配") @RequestParam(required=false) @Size(max=200) String title,
        @RequestParam(defaultValue="1") @javax.validation.constraints.Min(1) int page,
        @RequestParam(defaultValue="20") @javax.validation.constraints.Min(1) @javax.validation.constraints.Max(100) int pageSize){return service.list(status,applicant,owner,currentNode,title,page,pageSize);}
    @GetMapping("/{id}") public Ticket detail(@PathVariable @Size(max=32) String id){return service.get(id);}
    @io.swagger.v3.oas.annotations.Operation(summary="申请者撤销待审批工单，返回草稿")
    @PostMapping("/{id}/withdraw") public Ticket withdraw(@PathVariable @Size(max=32) String id,@Valid @RequestBody com.zhixing.ticket.model.WithdrawRequest request){return service.withdraw(id,request);}
    @io.swagger.v3.oas.annotations.Operation(summary="申请者永久删除草稿及流程记录，不删除附件文件")
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDraft(@PathVariable @Size(max=32) String id,@RequestParam @javax.validation.constraints.NotBlank @Size(max=64) String applicant){service.deleteDraft(id,applicant);}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public Ticket create(@Valid @RequestBody Ticket ticket){return service.create(ticket);}
    @PutMapping("/{id}") public Ticket update(@PathVariable @Size(max=32) String id,@Valid @RequestBody Ticket ticket){return service.update(id,ticket);}
    @PostMapping("/{id}/submit") public Ticket submit(@PathVariable @Size(max=32) String id){return service.submit(id);}
    @PostMapping("/{id}/approve") public Ticket approve(@PathVariable @Size(max=32) String id,@Valid @RequestBody ReviewRequest request){return service.approve(id,request);}
    @PostMapping("/{id}/reject") public Ticket reject(@PathVariable @Size(max=32) String id,@Valid @RequestBody ReviewRequest request){return service.reject(id,request);}
    @PostMapping("/{id}/complete-task") public Ticket completeTask(@PathVariable @Size(max=32) String id,@Valid @RequestBody ReviewRequest request){return service.completeTask(id,request);}

    @GetMapping("/workflow") public Map<String,Object> workflow(){Map<String,Object> result=new LinkedHashMap<String,Object>();result.put("nodes",new String[]{"填写申请","领导审批","目标知识库管理员审批","知识上传","验收审批","完成"});return result;}
}
