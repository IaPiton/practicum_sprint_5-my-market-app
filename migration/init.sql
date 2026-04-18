-- Таблица товаров (каталог)
CREATE TABLE items (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    img_path VARCHAR(500),
    price BIGINT NOT NULL CHECK (price >= 0),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Таблица корзины (сессионная или пользовательская)
CREATE TABLE cart (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Таблица позиций в корзине (связь многие-ко-многим с количеством)
CREATE TABLE cart_items (
    id BIGSERIAL PRIMARY KEY,
    cart_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    quantity INT NOT NULL DEFAULT 1 CHECK (quantity > 0),
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (cart_id) REFERENCES cart(id) ON DELETE CASCADE,
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE,
    UNIQUE(cart_id, item_id)
);

-- Таблица заказов
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(50) UNIQUE NOT NULL,
    total_sum BIGINT NOT NULL CHECK (total_sum >= 0),
    status VARCHAR(50) DEFAULT 'NEW',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Таблица позиций в заказе (снэпшот на момент покупки)
CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGSERIAL NOT NULL,
    item_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    price BIGINT NOT NULL CHECK (price >= 0),
    quantity INT NOT NULL CHECK (quantity > 0),
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE RESTRICT
);

-- Индексы для оптимизации запросов
CREATE INDEX idx_items_title ON items(title);
CREATE INDEX idx_items_price ON items(price);
CREATE INDEX idx_cart_items_cart_id ON cart_items(cart_id);
CREATE INDEX idx_cart_items_item_id ON cart_items(item_id);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_orders_created_at ON orders(created_at);
CREATE INDEX idx_orders_status ON orders(status);

-- Комментарии к таблицам и колонкам
COMMENT ON TABLE items IS 'Каталог товаров';
COMMENT ON COLUMN items.img_path IS 'Путь к изображению товара (например, /images/ball.jpg)';
COMMENT ON TABLE cart IS 'Корзина покупок';
COMMENT ON TABLE cart_items IS 'Позиции в корзине с количеством';
COMMENT ON TABLE orders IS 'Заказы пользователей';
COMMENT ON COLUMN orders.order_number IS 'Уникальный номер заказа в формате ORDER-{timestamp}';
COMMENT ON TABLE order_items IS 'Сохраненные позиции заказа';

INSERT INTO items (title, description, img_path, price) VALUES
('Мяч футбольный Nike Strike', 'Официальный футбольный мяч с улучшенной аэродинамикой. Подходит для игры на любых покрытиях.', 'images/nike-ball.jpg', 2990),
('Баскетбольный мяч Spalding', 'Профессиональный баскетбольный мяч для зала. Резиновая основа с велюровым покрытием.', 'images/spalding-ball.jpg', 1890),
('Теннисная ракетка Wilson', 'Легкая ракетка для начинающих и любителей. Увеличенная зона попадания.', 'images/wilson-racket.jpg', 3450),
('Беговые кроссовки Asics', 'Профессиональные беговые кроссовки с амортизацией Gel. Идеальны для длительных пробежек.', 'images/asics-shoes.jpg', 7990),
('Спортивная форма Adidas', 'Дышащая форма из полиэстера для тренировок и соревнований. В комплекте футболка и шорты.', 'images/adidas-kit.jpg', 4590),
('Гантели 5 кг', 'Набор из двух гантелей по 5 кг каждая. Эргономичные ручки, нескользящее покрытие.', 'images/dumbbells.jpg', 1990),
('Коврик для йоги', 'Противоскользящий коврик для йоги и фитнеса. Толщина 6 мм, размер 183x61 см.', 'images/yoga-mat.jpg', 1490),
('Прыгалка профессиональная', 'Скоростная скакалка с подшипниками и стальным тросом. Регулируемая длина.', 'images/jump-rope.jpg', 890),
('Бутылка для воды 1л', 'Термоизолированная бутылка из нержавеющей стали. Сохраняет температуру до 12 часов.', 'images/water-bottle.jpg', 1290),
('Фитнес-браслет Xiaomi', 'Трекер активности с пульсометром и мониторингом сна. Водонепроницаемый до 5 ATM.', 'images/mi-band.jpg', 3990),
('Эспандер кистевой', 'Регулируемый эспандер для развития силы кисти и предплечья. Сопротивление до 40 кг.', 'images/gripper.jpg', 590),
('Пояс для бега', 'Спортивный пояс на молнии для телефона и ключей. Светоотражающие элементы.', 'images/running-belt.jpg', 990),
('Напульники хлопковые', 'Впитывающие напульсники для защиты запястий и удаления пота. Пара, черный цвет.', 'images/wristbands.jpg', 299),
('Спортивные носки 3 пары', 'Компрессионные носки для бега и фитнеса. Антибактериальная пропитка.', 'images/socks.jpg', 699),
('Массажный ролл', 'МФР-ролл для восстановления мышц после тренировок. Плотность 50D.', 'images/foam-roller.jpg', 1490);

-- Таблица пользователей
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    phone VARCHAR(20),
    enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Таблица ролей
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(255)
);

-- Таблица связей пользователей с ролями (многие-ко-многим)
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);




-- Добавляем связь с пользователем в таблицу корзины
ALTER TABLE cart ADD COLUMN user_id BIGINT;
ALTER TABLE cart ADD CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;-- ACTIVE, CONVERTED_TO_ORDER


-- Добавляем связь с пользователем в таблицу заказов
ALTER TABLE orders ADD COLUMN user_id BIGINT;
ALTER TABLE orders ADD CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL;


-- Добавляем индексы для новых полей
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_cart_user_id ON cart(user_id);
CREATE INDEX idx_orders_user_id ON orders(user_id);

-- =====================================================
-- ВСТАВКА ДАННЫХ (РОЛИ И ТЕСТОВЫЕ ПОЛЬЗОВАТЕЛИ)
-- =====================================================

-- Вставка ролей
INSERT INTO roles (name, description) VALUES
('ROLE_USER', 'Обычный пользователь'),
('ROLE_ADMIN', 'Администратор системы'),
('ROLE_MANAGER', 'Менеджер магазина'),
('ROLE_ANONYMUS', 'Аннонимный пользователь');

-- Вставка тестовых пользователей (пароль: 'password' захеширован BCrypt)
-- Для тестов: password = '$2a$10$N.ZOn9J6.qPZc9O9QY2U8eF7XqZ3YxV5wW7rR8tT6uU9iI1oO2pP3S'
INSERT INTO public.users (id, username, email, "password", full_name, phone, enabled, created_at, updated_at) VALUES(3, 'user', 'user@example.com', '$2a$10$VsvBY7uJhSaI2FRSyfzQOONTWqynla/54KWB6HleoYLQY/dzLAvOi', 'Тестовый Пользователь', '+7 (999) 123-45-67', true, '2026-04-17 12:00:26.949', '2026-04-17 15:35:55.849');
INSERT INTO public.users (id, username, email, "password", full_name, phone, enabled, created_at, updated_at) VALUES(2, 'admin', 'admin@example.com', '$2a$10$VsvBY7uJhSaI2FRSyfzQOONTWqynla/54KWB6HleoYLQY/dzLAvOi', 'Администратор Системы', '+7 (999) 765-43-21', true, '2026-04-17 12:00:26.949', '2026-04-17 15:35:51.699');
INSERT INTO public.users (id, username, email, "password", full_name, phone, enabled, created_at, updated_at) VALUES(1, 'anonimys', 'anonimys@example.com', '$2a$10$VsvBY7uJhSaI2FRSyfzQOONTWqynla/54KWB6HleoYLQY/dzLAvOi', 'Администратор Системы', '+7 (999) 765-43-21', true, '2026-04-17 12:00:26.949', '2026-04-17 15:35:51.703');

-- Назначение ролей пользователям
INSERT INTO user_roles (user_id, role_id) VALUES
(1, 4), -- user -> ROLE_USER
(2, 2), -- admin -> ROLE_USER
(2, 3), -- admin -> ROLE_ADMIN
(3, 1);


COMMENT ON TABLE users IS 'Пользователи системы';
COMMENT ON TABLE roles IS 'Роли пользователей для авторизации';
COMMENT ON TABLE user_roles IS 'Связь пользователей с ролями';

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Триггеры для таблиц
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_items_updated_at BEFORE UPDATE ON items FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_cart_updated_at BEFORE UPDATE ON cart FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_cart_items_updated_at BEFORE UPDATE ON cart_items FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_orders_updated_at BEFORE UPDATE ON orders FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
