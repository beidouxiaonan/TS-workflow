package com.zhixing.ticket.model;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

public class TicketPage {
    @Schema(description="当前页工单列表") private final List<Ticket> items;
    @Schema(description="符合所有查询条件的总条数") private final long total;
    @Schema(description="页码，从1开始") private final int page;
    @Schema(description="每页条数，1至100") private final int pageSize;
    public TicketPage(List<Ticket> items,long total,int page,int pageSize){this.items=items;this.total=total;this.page=page;this.pageSize=pageSize;}
    public List<Ticket> getItems(){return items;}
    public long getTotal(){return total;}
    public int getPage(){return page;}
    public int getPageSize(){return pageSize;}
    @Schema(description="总页数，无结果时为0") public long getTotalPages(){return total/pageSize+(total%pageSize==0?0:1);}
}
