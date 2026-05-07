CREATE TABLE shipments (
       id UUID PRIMARY KEY,
       plantation_id UUID NOT NULL,
       mandor_id UUID NOT NULL,
       driver_id UUID,
       total_weight_kg DECIMAL(10, 2) NOT NULL,
       recognized_weight_kg DECIMAL(10, 2),
       status VARCHAR(50) NOT NULL,
       rejected_reason TEXT,
       created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
       updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE shipment_items (
    id UUID PRIMARY KEY,
    shipment_id UUID NOT NULL,
    harvest_id UUID NOT NULL,
    CONSTRAINT fk_shipment_items_shipment
        FOREIGN KEY (shipment_id)
        REFERENCES shipments (id)
        ON DELETE CASCADE
);