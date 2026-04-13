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
    session_id VARCHAR(100) UNIQUE,
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