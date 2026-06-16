UPDATE card_pool_transaction
SET currency = 'TND'
WHERE amount IS NOT NULL
  AND currency IS DISTINCT FROM 'TND';

UPDATE card_transaction
SET currency = 'TND'
WHERE amount IS NOT NULL
  AND currency IS DISTINCT FROM 'TND';
