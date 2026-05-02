-- 创建数据库
CREATE
DATABASE IF NOT EXISTS webchat DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE
webchat;

-- 订单表
CREATE TABLE IF NOT EXISTS orders
(
    id
    BIGINT
    PRIMARY
    KEY
    AUTO_INCREMENT
    COMMENT
    '主键ID',
    order_no
    VARCHAR
(
    64
) NOT NULL COMMENT '订单号',
    customer VARCHAR
(
    128
) COMMENT '客户名称',
    amount DECIMAL
(
    12,
    2
) COMMENT '订单金额',
    status VARCHAR
(
    32
) COMMENT '订单状态',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_order_no
(
    order_no
),
    INDEX idx_customer
(
    customer
),
    INDEX idx_created_at
(
    created_at
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 对话历史表
CREATE TABLE IF NOT EXISTS chat_history
(
    id
    BIGINT
    PRIMARY
    KEY
    AUTO_INCREMENT
    COMMENT
    '主键ID',
    session_id
    VARCHAR
(
    64
) NOT NULL COMMENT '会话ID',
    role VARCHAR
(
    16
) NOT NULL COMMENT '角色: user/assistant',
    content TEXT NOT NULL COMMENT '对话内容',
    query_conditions JSON COMMENT '查询条件JSON',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_session_id
(
    session_id
),
    INDEX idx_created_at
(
    created_at
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话历史表';

-- AI配置表
CREATE TABLE IF NOT EXISTS ai_config
(
    id
    BIGINT
    PRIMARY
    KEY
    AUTO_INCREMENT
    COMMENT
    '主键ID',
    config_key
    VARCHAR
(
    64
) NOT NULL UNIQUE COMMENT '配置键',
    config_value TEXT COMMENT '配置值',
    description VARCHAR
(
    256
) COMMENT '配置描述',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI配置表';

-- 初始化配置数据
INSERT INTO ai_config (config_key, config_value, description)
VALUES ('system_prompt', '你是一个数据分析助手，可以根据提供的业务数据回答用户问题。', '系统提示词'),
       ('model_name', 'MiniMax-Text-01', '模型名称');

-- 初始化测试订单数据
INSERT INTO orders (order_no, customer, amount, status, created_at)
VALUES ('ORD202401001', '张三', 1500.00, '已完成', '2024-01-15 10:30:00'),
       ('ORD202401002', '李四', 2800.00, '处理中', '2024-01-16 14:20:00'),
       ('ORD202401003', '王五', 999.00, '已取消', '2024-01-17 09:15:00'),
       ('ORD202401004', '张三', 5600.00, '已完成', '2024-01-18 16:45:00'),
       ('ORD202401005', '赵六', 3200.00, '已完成', '2024-01-19 11:00:00');
