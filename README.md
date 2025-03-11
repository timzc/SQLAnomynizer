# 数据脱敏工具

一个Java应用程序，用于连接MySQL数据库并根据可配置的规则对表中的敏感数据进行脱敏处理。

## 功能特点

- 可配置的数据库连接（URL、用户名、密码）
- 支持同时处理多个表
- 针对不同类型敏感数据的脱敏规则：
  - 银行卡号
  - 身份证号码（包括军官证号码、护照号码）
  - 姓名
  - 手机号码（支持不同地区的规则）
  - 固定电话号码
  - 电子邮箱
  - 金额数据
  - 文本数据

## 脱敏规则

应用程序实现了以下脱敏规则：

1. **银行卡号**：将所有数字和字母替换为随机数字和字母，保持长度一致
   - 示例：原始值：6225751234567891496 → 脱敏后：7814329876543217658

2. **身份证号码**：将所有数字和字母替换为随机数字和字母，保持长度一致
   - 示例：原始值：110101199003071234 → 脱敏后：392857200916483765

3. **姓名**：根据不同文字类型替换为相应的随机字符
   - 中文字符替换为随机中文字符
   - 日文字符替换为随机日文字符
   - 韩文字符替换为随机韩文字符
   - 拉丁字母（英文等）替换为随机拉丁字母（保持大小写）
   - 示例：原始值：张三 → 脱敏后：李四（随机中文字符）
   - 示例：原始值：John Smith → 脱敏后：Kqtf Pbrxm（随机拉丁字符）
   - 示例：混合姓名如 张John → 每个字符按其类型进行相应的随机替换

4. **手机号码**：将所有数字替换为随机数字，保持长度一致
   - 示例：原始值：13712349050 → 脱敏后：28945671032

5. **固定电话号码**：将所有数字替换为随机数字，保持长度一致
   - 示例：原始值：010-12345678 → 脱敏后：023-45678901

6. **电子邮箱**：保持邮箱格式，将字母替换为随机字母
   - 示例：原始值：zhangsan@example.com → 脱敏后：kqtfpbr@ixample.com

7. **金额**：将金额替换为随机数值，不要求保持长度一致，但保留货币符号和格式
   - 示例：原始值：¥1234.56 → 脱敏后：¥789.32
   - 示例：原始值：$9876.54 → 脱敏后：$5421.87
   - 示例：原始值：1000.00元 → 脱敏后：6358.42元

8. **文本**：将文本替换为随机字符，保持单词结构
   - 示例：原始值：北京市海淀区 → 脱敏后：南京市朝阳区
   - 示例：原始值：Product Description → 脱敏后：Qvstufx Fynvmqcxsrb

## 系统要求

- Java 11 或更高版本
- MySQL 数据库
- Maven

## 配置说明

编辑 `src/main/resources/config.properties` 文件来配置应用程序。新版本支持同时处理多个表：

```properties
# 数据库配置
db.url=jdbc:mysql://localhost:3306/
db.name=your_database_name
db.user=your_username
db.password=your_password

# 表配置 (逗号分隔的表名列表)
tables=customers,orders,transactions

# 每个表的脱敏配置
# 表名.columns 指定该表需要脱敏的列（逗号分隔）
customers.columns=bank_card,id_card,name,mobile,phone,email,amount
orders.columns=customer_name,shipping_address,phone
transactions.columns=card_number,amount,description

# 列类型配置（用于确定脱敏规则）
# 格式: column.type.表名.列名=类型
# customers表列类型
column.type.customers.bank_card=BANK_CARD
column.type.customers.id_card=ID_CARD
column.type.customers.name=NAME
column.type.customers.mobile=MOBILE
column.type.customers.phone=PHONE
column.type.customers.email=EMAIL
column.type.customers.amount=AMOUNT

# orders表列类型
column.type.orders.customer_name=NAME
column.type.orders.shipping_address=TEXT
column.type.orders.phone=PHONE

# transactions表列类型
column.type.transactions.card_number=BANK_CARD
column.type.transactions.amount=AMOUNT
column.type.transactions.description=TEXT
```

## 构建应用

```bash
mvn clean package
```

这将在 `target` 目录中创建一个包含所有依赖的JAR文件。

## 运行应用

```bash
java -jar target/data-anonymizer-1.0-SNAPSHOT-jar-with-dependencies.jar
```

## 示例数据库设置

以下是创建多个测试表和示例数据的SQL脚本：

```sql
CREATE DATABASE test_anonymizer;
USE test_anonymizer;

-- 客户表
CREATE TABLE customers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100),
    id_card VARCHAR(18),
    bank_card VARCHAR(19),
    mobile VARCHAR(20),
    phone VARCHAR(20),
    email VARCHAR(100),
    amount DECIMAL(10,2)
);

INSERT INTO customers (name, id_card, bank_card, mobile, phone, email, amount)
VALUES 
    ('张三', '110101199003071234', '6225751234567891496', '13712349050', '010-12345678', 'zhangsan@example.com', 1234.56),
    ('李四', '310101199107081234', '6225752345678912345', '15887654321', '021-87654321', 'lisi@example.com', 9876.54),
    ('王五', '440101199209091234', '6225753456789123456', '13612345678', '020-12345678', 'wangwu@example.com', 5000.00),
    ('陈六', '510101199310101234', '6225754567891234567', '90123456', '852-12345678', 'chenliu@example.com', 12345.67),
    ('林七', '710101199411111234', '6225755678912345678', '0912345678', '886-12345678', 'linqi@example.com', 789.12);

-- 订单表
CREATE TABLE orders (
    order_id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT,
    customer_name VARCHAR(100),
    shipping_address VARCHAR(200),
    phone VARCHAR(20),
    order_date DATETIME,
    total_amount DECIMAL(10,2),
    FOREIGN KEY (customer_id) REFERENCES customers(id)
);

INSERT INTO orders (customer_id, customer_name, shipping_address, phone, order_date, total_amount)
VALUES
    (1, '张三', '北京市海淀区中关村大街1号', '13712349050', '2023-01-15 14:30:00', 299.99),
    (2, '李四', '上海市浦东新区张江高科技园区', '15887654321', '2023-02-20 10:15:00', 1299.50),
    (3, '王五', '广州市天河区天河路385号', '13612345678', '2023-03-05 16:45:00', 459.90),
    (1, '张三', '北京市朝阳区建国路89号', '13712349050', '2023-04-10 09:20:00', 699.00);

-- 交易表
CREATE TABLE transactions (
    transaction_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT,
    card_number VARCHAR(19),
    amount DECIMAL(10,2),
    transaction_date DATETIME,
    description VARCHAR(200),
    status VARCHAR(20),
    FOREIGN KEY (order_id) REFERENCES orders(order_id)
);

INSERT INTO transactions (order_id, card_number, amount, transaction_date, description, status)
VALUES
    (1, '6225751234567891496', 299.99, '2023-01-15 14:35:22', '购买电子产品', 'SUCCESS'),
    (2, '6225752345678912345', 1299.50, '2023-02-20 10:18:45', '购买家用电器', 'SUCCESS'),
    (3, '6225753456789123456', 459.90, '2023-03-05 16:50:12', '购买服装', 'SUCCESS'),
    (4, '6225751234567891496', 699.00, '2023-04-10 09:25:33', '购买数码产品', 'SUCCESS');
```

## 许可证

本项目采用MIT许可证。 