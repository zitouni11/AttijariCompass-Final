WITH adam_cards(catalog_code, card_holder_name, card_number, expiry_month, expiry_year, cvv, masked_card_number, last4, card_code) AS (
    VALUES
    ('CARTE_PLATINUM',            'ADAM ZITOUNI', '5378123400009011', 10, 2029, '315', '**** **** **** 9011', '9011', 'PLAT-ADAM-01'),
    ('CARTE_GOLD_NATIONALE',      'ADAM ZITOUNI', '5299456700007712',  8, 2027, '907', '**** **** **** 7712', '7712', 'GOLDN-ADAM-01'),
    ('CARTE_GOLD_INTERNATIONALE', 'ADAM ZITOUNI', '5299456700006623',  6, 2029, '614', '**** **** **** 6623', '6623', 'GOLDI-ADAM-01'),
    ('CARTE_VISA_NATIONALE',      'ADAM ZITOUNI', '4111222200005544', 11, 2028, '552', '**** **** **** 5544', '5544', 'VNAT-ADAM-01'),
    ('CARTE_VISA_INTERNATIONALE', 'ADAM ZITOUNI', '4556332200007788',  9, 2029, '228', '**** **** **** 7788', '7788', 'VINT-ADAM-01'),
    ('CARTE_CIB',                 'ADAM ZITOUNI', '6270101234567890',  5, 2028, '390', '**** **** **** 7890', '7890', 'CIB-ADAM-01'),
    ('CARTE_TAWA_TAWA',           'ADAM ZITOUNI', '4023555500008888',  7, 2027, '118', '**** **** **** 8888', '8888', 'TAWA-ADAM-01'),
    ('CARTE_IDDIKHAR',            'ADAM ZITOUNI', '4012888800002234',  5, 2029, '204', '**** **** **** 2234', '2234', 'IDDI-ADAM-01'),
    ('CARTE_VOYAGE',              'ADAM ZITOUNI', '4123456700003456',  6, 2029, '742', '**** **** **** 3456', '3456', 'VOY-ADAM-01'),
    ('CARTE_OULIDHA',             'ADAM ZITOUNI', '4066554400001100',  2, 2027, '681', '**** **** **** 1100', '1100', 'OULI-ADAM-01'),
    ('CARTE_TECHNOLOGIQUE',       'ADAM ZITOUNI', '4567987600001122',  4, 2028, '245', '**** **** **** 1122', '1122', 'TECH-ADAM-01'),
    ('CARTE_AVENIR',              'ADAM ZITOUNI', '4532111100009876',  1, 2029, '826', '**** **** **** 9876', '9876', 'AVEN-ADAM-01')
)
INSERT INTO public.card_pool
(
    card_catalog_id,
    card_holder_name,
    card_number,
    expiry_month,
    expiry_year,
    cvv,
    masked_card_number,
    last4,
    card_code,
    assigned,
    created_at,
    updated_at
)
SELECT
    cc.id,
    ac.card_holder_name,
    ac.card_number,
    ac.expiry_month,
    ac.expiry_year,
    ac.cvv,
    ac.masked_card_number,
    ac.last4,
    ac.card_code,
    false,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM adam_cards ac
JOIN public.card_catalog cc ON cc.code = ac.catalog_code
WHERE NOT EXISTS (
    SELECT 1
    FROM public.card_pool cp
    WHERE cp.card_code = ac.card_code
);
