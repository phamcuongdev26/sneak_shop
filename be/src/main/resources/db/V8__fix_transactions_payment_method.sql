UPDATE transactions SET payment_method = 'e_wallet' WHERE payment_method NOT IN ('cod', 'bank_transfer', 'e_wallet');
