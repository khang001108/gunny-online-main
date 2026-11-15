'use strict';
const supportStorage = 'gunny-support-pending';
let pendingSupport = null;
try { pendingSupport = JSON.parse(sessionStorage.getItem(supportStorage) || 'null'); } catch (_) {}

function restoreSupport() {
    $('supportPending').hidden = !pendingSupport;
    if (!pendingSupport) return;
    const option = node('option', pendingSupport.label);
    option.value = pendingSupport.data.id;
    $('supportPlayer').replaceChildren(option);
    for (const field of ['amount', 'comment', 'stones', 'coins', 'money']) {
        $('support' + field[0].toUpperCase() + field.slice(1)).value = pendingSupport.data[field];
    }
}

async function loadSupport() {
    restoreSupport();
    const entries = await api('support-list');
    $('supportEntries').replaceChildren();
    for (const entry of entries) {
        const card = node('article', undefined, 'notice-card');
        card.append(node('h3', entry.display_name + ' · ' + Number(entry.amount).toLocaleString('vi-VN') + ' đ'));
        card.append(node('p', entry.comment));
        card.append(node('small', stamp(entry.created_at)));
        card.append(node('p', 'Quà riêng tư: ' + entry.stones + ' đá · ' + entry.coins + ' xu · ' + entry.money + ' tiền mua sắm'));
        const edit = node('button', 'Sửa bình luận');
        edit.onclick = async () => {
            const comment = prompt('Bình luận công khai', entry.comment);
            if (comment === null) return;
            try {
                await api('support-comment', {request_key: entry.request_key, comment});
                await loadSupport();
                feedback('Đã sửa bình luận');
            } catch (error) { feedback(error.message); }
        };
        card.append(edit);
        $('supportEntries').append(card);
    }
    if (!entries.length) $('supportEntries').append(node('p', 'Chưa có ghi nhận ủng hộ.'));
    $('supportQrPreview').src = '/download/support-qr.jpg?v=' + Date.now();
}

$('supportRefresh').onclick = () => loadSupport().catch(error => feedback(error.message));
$('supportSearchForm').onsubmit = async event => {
    event.preventDefault();
    if (pendingSupport) { feedback('Hãy xác nhận yêu cầu đang chờ trước khi chọn người khác.'); return; }
    await busy(event.target, async () => {
        try {
            const result = await api('accounts?offset=0&search=' + encodeURIComponent($('supportSearch').value.trim()));
            $('supportPlayer').replaceChildren();
            for (const account of result.accounts) {
                const option = node('option', account.display_name + ' · #' + account.public_id + ' · ' + account.email);
                option.value = account.id;
                $('supportPlayer').append(option);
            }
            if (!result.accounts.length) {
                const option = node('option', 'Không tìm thấy tài khoản');
                option.value = '';
                $('supportPlayer').append(option);
            }
        } catch (error) { feedback(error.message); }
    });
};

$('supportQrForm').onsubmit = async event => {
    event.preventDefault();
    await busy(event.target, async () => {
        try {
            const file = $('supportQrFile').files[0];
            if (!file || !['image/jpeg', 'image/png'].includes(file.type) || file.size > 1400000)
                throw Error('Chọn ảnh JPEG/PNG tối đa 1,4 MB.');
            const encoded = await new Promise((resolve, reject) => {
                const reader = new FileReader();
                reader.onload = () => resolve(reader.result.split(',')[1]);
                reader.onerror = () => reject(Error('Không đọc được ảnh.'));
                reader.readAsDataURL(file);
            });
            await api('support-qr', {qr: encoded, mime: file.type});
            $('supportQrPreview').src = '/download/support-qr.jpg?v=' + Date.now();
            $('supportQrFile').value = '';
            feedback('Đã cập nhật ảnh QR trên trang tải');
        } catch (error) { feedback(error.message); }
    });
};

$('supportEntryForm').onsubmit = async event => {
    event.preventDefault();
    await busy(event.target, async () => {
        try {
            const data = {id: $('supportPlayer').value};
            for (const field of ['amount', 'stones', 'coins', 'money']) {
                const value = Number($('support' + field[0].toUpperCase() + field.slice(1)).value);
                const max = field === 'amount' ? 1000000000000 : 999999;
                if (!Number.isSafeInteger(value) || value < 0 || value > max) throw Error('Kiểm tra số tiền và quà.');
                data[field] = value;
            }
            data.comment = $('supportComment').value.trim();
            if (!data.id) throw Error('Tìm và chọn tài khoản nhận quà.');
            if (pendingSupport) {
                const previous = {...pendingSupport.data};
                delete previous.request_key;
                if (JSON.stringify(previous) !== JSON.stringify(data)) {
                    restoreSupport();
                    throw Error('Đã khôi phục yêu cầu trước. Bấm lại để xác nhận kết quả.');
                }
            } else {
                if (!confirm('Ghi nhận ủng hộ và cộng quà vào ' + $('supportPlayer').selectedOptions[0].textContent + '?')) return;
                data.request_key = crypto.randomUUID();
                pendingSupport = {data, label: $('supportPlayer').selectedOptions[0].textContent};
                sessionStorage.setItem(supportStorage, JSON.stringify(pendingSupport));
            }
            $('supportPending').hidden = false;
            await api('support-add', pendingSupport.data);
            pendingSupport = null;
            sessionStorage.removeItem(supportStorage);
            $('supportEntryForm').reset();
            $('supportPending').hidden = true;
            feedback('Đã ghi nhận và gửi quà cảm ơn');
            await loadSupport();
        } catch (error) {
            if (error.status === 400) {
                pendingSupport = null;
                sessionStorage.removeItem(supportStorage);
                $('supportPending').hidden = true;
            }
            feedback(error.message);
        }
    });
};
