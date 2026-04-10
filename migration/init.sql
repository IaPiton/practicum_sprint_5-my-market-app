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
COMMENT ON TABLE order_items IS 'Сохраненные позиции заказа (снэпшот)';