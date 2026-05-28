package com.huang.zhixing.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("bi_user")
public class BiUser {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
