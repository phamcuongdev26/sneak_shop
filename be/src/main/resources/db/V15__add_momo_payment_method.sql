UPDATE transactions
SET payment_method = 'e_wallet'
WHERE payment_method NOT IN ('cod', 'bank_transfer', 'e_wallet', 'momo');

ALTER TABLE orders
  MODIFY COLUMN payment_method ENUM('cod','bank_transfer','e_wallet','momo') NOT NULL;

ALTER TABLE transactions
  MODIFY COLUMN payment_method ENUM('cod','bank_transfer','e_wallet','momo') NOT NULL;
