-- system_scopes seed data (INSERT IGNORE: 재시작 시 중복 무시)
INSERT IGNORE INTO system_scopes (scope_code, category, description, created_at) VALUES
  ('product:read',    'PRODUCT',    '상품 조회',    NOW()),
  ('product:write',   'PRODUCT',    '상품 등록/수정', NOW()),
  ('product:delete',  'PRODUCT',    '상품 삭제',    NOW()),
  ('order:read',      'ORDER',      '주문 조회',    NOW()),
  ('order:write',     'ORDER',      '주문 처리',    NOW()),
  ('claim:read',      'CLAIM',      '클레임 조회',  NOW()),
  ('claim:update',    'CLAIM',      '클레임 처리',  NOW()),
  ('stats:read',      'STATS',      '통계 조회',    NOW()),
  ('marketing:read',  'MARKETING',  '마케팅 조회',  NOW()),
  ('marketing:write', 'MARKETING',  '마케팅 실행',  NOW()),
  ('settlement:read', 'SETTLEMENT', '정산 조회',    NOW());
