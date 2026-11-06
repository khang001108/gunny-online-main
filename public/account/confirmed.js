'use strict';
(async function () {
  const title = document.getElementById('title');
  const message = document.getElementById('message');
  const seal = document.getElementById('seal');
  const query = new URLSearchParams(location.search);
  const fragment = new URLSearchParams(location.hash.slice(1));
  const access = fragment.get('access_token');
  const token = query.get('token_hash');
  const error = query.get('error') || query.get('error_description') || fragment.get('error') || fragment.get('error_description');
  // Default Supabase emails verify first, then return a session in the fragment.
  // Keep old TokenHash links working too; neither flow asks for another click.
  history.replaceState(null, '', location.pathname);
  if (error || (!access && !token)) {
    seal.textContent = '!';
    title.textContent = error ? 'Liên kết đã hết hạn hoặc đã sử dụng' : 'Hãy mở liên kết xác nhận trong email';
    message.textContent = 'Nếu bạn đã xác nhận trước đó, hãy quay lại game để đăng nhập. Nếu chưa, hãy dùng thư xác nhận mới nhất.';
    return;
  }
  try {
    const response = await fetch('/auth/api/' + (access ? 'verify-access' : 'verify'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'omit',
      body: JSON.stringify(access ? { access_token: access } : { token_hash: token })
    });
    const result = await response.json();
    if (!response.ok || result.confirmed !== true) throw new Error('unconfirmed');
    seal.textContent = '✓';
    title.textContent = 'Chúc mừng bạn đăng ký thành công!';
    message.textContent = 'Email của bạn đã được xác nhận. Chào mừng bạn đến với Gunny Java!';
    document.getElementById('return').hidden = false;
    document.title = 'Đăng ký thành công · Gunny Java';
  } catch (_) {
    seal.textContent = '!';
    title.textContent = 'Chưa kiểm tra được kết quả xác nhận';
    message.textContent = 'Bạn có thể quay lại game và thử đăng nhập. Nếu email chưa được xác nhận, hãy mở thư xác nhận mới nhất.';
  }
}());
