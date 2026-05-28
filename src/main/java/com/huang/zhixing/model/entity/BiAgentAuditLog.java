package com.huang.zhixing.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("bi_agent_audit_log")
public class BiAgentAuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String userId;

    private String sessionId;

    private String userPrompt;

    private String aiResponse;

    private Long costTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
