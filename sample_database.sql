-- Sample database setup for Data Anonymizer testing

-- Create database
CREATE DATABASE IF NOT EXISTS test_anonymizer;
USE test_anonymizer;

-- 客户表
CREATE TABLE IF NOT EXISTS customers (
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
CREATE TABLE IF NOT EXISTS orders (
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
CREATE TABLE IF NOT EXISTS transactions (
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

-- Create a sample config for this database
-- Copy these values to your config.properties file
SELECT 
    'Database setup complete. Use the following configuration:',
    'db.url=jdbc:mysql://localhost:3306/',
    'db.name=test_anonymizer',
    'db.user=your_username',
    'db.password=your_password',
    'tables=customers,orders,transactions',
    'customers.columns=bank_card,id_card,name,mobile,phone,email,amount',
    'orders.columns=customer_name,shipping_address,phone',
    'transactions.columns=card_number,amount,description'; 