const API_BASE = '/api/contacts';
let allContacts = []; // 用于搜索

// 渲染顶部按钮
function renderActionBar() {
  const bar = document.getElementById('actionBar');
  bar.innerHTML = `
    <button class="btn btn-sm btn-success" onclick="openAddModal()">
      <i class="fas fa-plus me-1"></i>添加
    </button>
    <button class="btn btn-sm btn-outline-secondary" onclick="loadContacts()">全部联系人</button>
    <button class="btn btn-sm btn-outline-warning" onclick="loadBookmarked()">收藏联系人</button>
    <a href="${API_BASE}/export" class="btn btn-sm btn-outline-info">
      <i class="fas fa-file-excel me-1"></i>导出 Excel
    </a>
    <button class="btn btn-sm btn-outline-info" onclick="document.getElementById('importFile').click()">
      <i class="fas fa-upload me-1"></i>导入 Excel
    </button>
    <input type="file" id="importFile" accept=".xlsx,.xls" style="display:none" onchange="importExcel(this.files[0])">
  `;
}

// 创建联系方式行
function createMethodRow(type = 'phone', value = '') {
  const div = document.createElement('div');
  div.className = 'input-group mb-2 method-row';
  div.innerHTML = `
    <select class="form-select" name="methodType">
      <option value="phone" ${type === 'phone' ? 'selected' : ''}>电话</option>
      <option value="email" ${type === 'email' ? 'selected' : ''}>邮箱</option>
      <option value="wechat" ${type === 'wechat' ? 'selected' : ''}>微信</option>
      <option value="address" ${type === 'address' ? 'selected' : ''}>地址</option>
    </select>
    <input type="text" class="form-control" name="value" value="${value}" placeholder="请输入值" required>
    <button type="button" class="btn btn-sm btn-danger remove-method">删除</button>
  `;
  div.querySelector('.remove-method').onclick = () => div.remove();
  return div;
}

// 打开添加模态框
function openAddModal() {
  document.getElementById('modalTitle').textContent = '添加联系人';
  document.getElementById('contactId').value = '';
  document.getElementById('name').value = '';
  const container = document.getElementById('methodsContainer');
  container.innerHTML = '';
  container.appendChild(createMethodRow());
  new bootstrap.Modal(document.getElementById('contactModal')).show();
}

// 打开编辑模态框
function openEditModal(contact) {
  document.getElementById('modalTitle').textContent = '编辑联系人';
  document.getElementById('contactId').value = contact.id;
  document.getElementById('name').value = contact.name || '';
  
  const container = document.getElementById('methodsContainer');
  container.innerHTML = '';
  
  if (contact.methods && contact.methods.length > 0) {
    contact.methods.forEach(m => {
      container.appendChild(createMethodRow(m.methodType, m.value));
    });
  } else {
    container.appendChild(createMethodRow());
  }
  
  new bootstrap.Modal(document.getElementById('contactModal')).show();
}

// 保存联系人（新增或更新）
async function saveContact() {
  const id = document.getElementById('contactId').value;
  const name = document.getElementById('name').value.trim();
  if (!name) {
    alert('请输入姓名');
    return;
  }

  const methods = [];
  document.querySelectorAll('#methodsContainer .method-row').forEach(row => {
    const type = row.querySelector('select[name="methodType"]').value;
    const value = row.querySelector('input[name="value"]').value.trim();
    if (value) methods.push({ methodType: type, value });
  });

  if (methods.length === 0) {
    alert('请至少添加一个联系方式');
    return;
  }

  const contact = { name, methods, bookmarked: false };

  try {
    let response;
    if (id) {
      // 更新
      response = await fetch(`${API_BASE}/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(contact)
      });
    } else {
      // 新增
      response = await fetch(API_BASE, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(contact)
      });
    }

    if (response.ok) {
      alert(id ? '更新成功！' : '添加成功！');
      loadContacts();
      bootstrap.Modal.getInstance(document.getElementById('contactModal')).hide();
    } else {
      const error = await response.text();
      alert('操作失败：' + error);
    }
  } catch (err) {
    alert('网络错误：' + err.message);
  }
}

// 提取方法值（用于表格显示）
function extractValue(methods, type) {
  if (!methods) return '';
  return methods
    .filter(m => m.methodType.toLowerCase() === type.toLowerCase())
    .map(m => m.value)
    .join('; ');
}

// 渲染联系人列表（支持搜索）
function renderContacts(contacts) {
  allContacts = contacts; // 保存原始数据用于搜索
  applySearch();
}

// 应用搜索过滤
function applySearch() {
  const keyword = document.getElementById('searchInput').value.toLowerCase().trim();
  let filtered = allContacts;

  if (keyword) {
    filtered = allContacts.filter(c => {
      const nameMatch = c.name?.toLowerCase().includes(keyword);
      const phoneMatch = extractValue(c.methods, 'phone').toLowerCase().includes(keyword);
      const emailMatch = extractValue(c.methods, 'email').toLowerCase().includes(keyword);
      const wechatMatch = extractValue(c.methods, 'wechat').toLowerCase().includes(keyword);
      const addressMatch = extractValue(c.methods, 'address').toLowerCase().includes(keyword);
      return nameMatch || phoneMatch || emailMatch || wechatMatch || addressMatch;
    });
  }

  const tbody = document.getElementById('contactTableBody');
  if (!filtered || filtered.length === 0) {
    tbody.innerHTML = `<tr class="empty-row"><td colspan="8">暂无数据</td></tr>`;
    return;
  }

  tbody.innerHTML = filtered.map(c => {
    const phone = extractValue(c.methods, 'phone');
    const email = extractValue(c.methods, 'email');
    const wechat = extractValue(c.methods, 'wechat');
    const address = extractValue(c.methods, 'address');

    return `
      <tr>
        <td>${c.id || ''}</td>
        <td>${c.name || ''}</td>
        <td>${phone}</td>
        <td>${email}</td>
        <td>${wechat}</td>
        <td>${address}</td>
        <td>
          ${c.bookmarked 
            ? '<i class="fas fa-star text-warning"></i>' 
            : '<i class="far fa-star text-muted"></i>'}
        </td>
        <td>
          <button class="btn btn-sm btn-outline-primary me-1" onclick="openEditModal(${JSON.stringify(c).replace(/"/g, '&quot;')})">编辑</button>
          <button class="btn btn-sm btn-outline-primary me-1" onclick="toggleBookmark(${c.id}, ${!c.bookmarked})">
            ${c.bookmarked ? '取消收藏' : '收藏'}
          </button>
          <button class="btn btn-sm btn-outline-danger" onclick="deleteContact(${c.id})">删除</button>
        </td>
      </tr>
    `;
  }).join('');
}

// 加载全部联系人
async function loadContacts() {
  try {
    const res = await fetch(API_BASE);
    const contacts = await res.json();
    renderContacts(contacts);
  } catch (err) {
    showError('加载失败: ' + err.message);
  }
}

// 加载收藏联系人
async function loadBookmarked() {
  try {
    const res = await fetch(`${API_BASE}/bookmarked`);
    const contacts = await res.json();
    renderContacts(contacts);
  } catch (err) {
    showError('加载收藏失败: ' + err.message);
  }
}

// 切换收藏
async function toggleBookmark(id, bookmarked) {
  try {
    await fetch(`${API_BASE}/${id}/bookmark`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ bookmarked })
    });
    loadContacts();
  } catch (err) {
    alert('操作失败: ' + err.message);
  }
}

// 删除联系人
async function deleteContact(id) {
  if (!confirm('确定删除？')) return;
  try {
    await fetch(`${API_BASE}/${id}`, { method: 'DELETE' });
    loadContacts();
  } catch (err) {
    alert('删除失败: ' + err.message);
  }
}

// 导入 Excel
async function importExcel(file) {
  if (!file) return;
  const formData = new FormData();
  formData.append('file', file);
  try {
    const res = await fetch(`${API_BASE}/import`, { method: 'POST', body: formData });
    const msg = await res.text();
    alert(msg);
    loadContacts();
  } catch (err) {
    alert('导入失败: ' + err.message);
  }
  document.getElementById('importFile').value = '';
}

// 显示错误
function showError(msg) {
  document.getElementById('contactTableBody').innerHTML = 
    `<tr><td colspan="8" class="text-center text-danger">${msg}</td></tr>`;
}

// 初始化
document.addEventListener('DOMContentLoaded', () => {
  renderActionBar();
  loadContacts();

  // 绑定搜索事件
  document.getElementById('searchInput').addEventListener('input', applySearch);

  // 绑定添加联系方式按钮
  document.querySelector('.add-method').addEventListener('click', () => {
    document.getElementById('methodsContainer').appendChild(createMethodRow());
  });
});