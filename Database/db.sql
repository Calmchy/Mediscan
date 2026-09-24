-- mediscan pos - simplified database schema (mariadb)
-- point-of-sale system for retail pharmacies

create database mediscan_pos;
use mediscan_pos;

-- users (staff / pharmacists / admin)
create table users (
    user_id int auto_increment primary key,
    username varchar(50) not null unique,
    password varchar(255) not null,
    first_name varchar(50) not null,
    last_name varchar(50) not null,
    middle_initial char(50),
    role enum('admin', 'pharmacist', 'cashier') not null default 'cashier',
    created_by int default null,
    created_at timestamp default current_timestamp,
    foreign key (created_by) references users(user_id)
        on delete set null on update cascade
);

-- categories
create table categories (
    category_id int auto_increment primary key,
    category_name varchar(100) not null unique
);

-- products (basic product info only, no stock/quantity)
-- product_id doubles as the barcode value (scan it directly)
create table products (
    product_id int auto_increment primary key,
    name varchar(150) not null,
    brand varchar(100),
    category_id int,
    cost_price decimal(10,2) not null default 0.00,
    selling_price decimal(10,2) not null default 0.00,
    requires_prescription boolean not null default false,
    is_age_restricted boolean not null default false,
    minimum_age int default null,
	unit_type enum('tablet', 'capsule', 'syrup', 'ml', 'box', 'piece') not null default 'piece',
	pack_size int default null,
    foreign key (category_id) references categories(category_id)
        on delete set null on update cascade
);

-- product stock (one product can have many stock batches)
create table product_stock (
    stock_id int auto_increment primary key,
    product_id int not null,
    batch_no varchar(50),
    quantity int not null default 0,
    expiry_date date,
    date_received date default (current_date),
    foreign key (product_id) references products(product_id)
        on delete cascade on update cascade
);

-- customers
create table customers (
    customer_id int auto_increment primary key,
    first_name varchar(50) not null,
    last_name varchar(50) not null,
    middle_initial char(50),
    date_of_birth date,
    email varchar(100),
    phone_number varchar(20),
    created_by int default null,
    created_at timestamp default current_timestamp
);

-- prescriptions (doctor info kept simple as plain fields)
create table prescriptions (
    prescription_id int auto_increment primary key,
    customer_id int not null,
    product_id int not null,
    doctor_name varchar(100),
    dosage varchar(100),
    quantity_prescribed int not null,
    date_issued date not null,
    status enum('pending', 'ready', 'picked_up') not null default 'pending',
    foreign key (customer_id) references customers(customer_id)
        on delete cascade on update cascade,
    foreign key (product_id) references products(product_id)
        on delete restrict on update cascade
);

-- refill reminders
create table refill_reminders (
    reminder_id int auto_increment primary key,
    prescription_id int not null,
    reminder_date date not null,
    method enum('sms', 'email') not null,
    sent_status enum('pending', 'sent') not null default 'pending',
    foreign key (prescription_id) references prescriptions(prescription_id)
        on delete cascade on update cascade
);

-- orders (the receipt)
create table orders (
    order_id int auto_increment primary key,
    user_id int not null,
    customer_id int,
    order_date datetime default current_timestamp,
    total_amount decimal(10,2) not null default 0.00,
    payment_method enum('cash', 'card', 'insurance') not null default 'cash',
    foreign key (user_id) references users(user_id)
        on delete restrict on update cascade,
    foreign key (customer_id) references customers(customer_id)
        on delete set null on update cascade
);

-- order items (how many of each product in that order)
create table order_items (
    order_item_id int auto_increment primary key,
    order_id int not null,
    product_id int not null,
    quantity int not null,
    unit_price decimal(10,2) not null,
    subtotal decimal(10,2) not null,
    id_verified boolean not null default false,
    foreign key (order_id) references orders(order_id)
        on delete cascade on update cascade,
    foreign key (product_id) references products(product_id)
        on delete restrict on update cascade
);

-- helpful indexes
create index idx_prescriptions_status on prescriptions(status);
create index idx_stock_expiry on product_stock(product_id, expiry_date);