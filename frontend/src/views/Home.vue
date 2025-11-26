<template>
  <div class="home-page">
    <section class="hero card">
      <div class="hero-content">
        <span class="pill pill-success">智能处方 · 医患协同</span>
        <h1>NEU 智慧处方工作台</h1>
        <p>
          构建“患者 AI 处方 + 医生审核”的统一门户。
          患者可以即时与 AI 交流生成处方，医生可在同一界面完成审核与追踪。
        </p>
        <div class="hero-actions">
          <button class="btn btn-primary" @click="activeTab = 'patient'">开始 AI 问诊</button>
          <button class="btn btn-secondary" @click="activeTab = 'doctor'">进入医生审核台</button>
        </div>
      </div>
      <div class="hero-visual float">
        <div class="pulse-ring"></div>
        <div class="hero-chat-preview card">
          <h4>AI 处方对话预览</h4>
          <ul>
            <li>
              <span class="pill pill-success">患者</span>
              最近两天持续发热 38.5℃，伴随咳嗽
            </li>
            <li>
              <span class="pill pill-warning">AI</span>
              已记录症状，请补充既往病史与用药情况。
            </li>
            <li>
              <span class="pill pill-success">患者</span>
              有鼻炎史，近期服用维生素 C，无药物过敏
            </li>
          </ul>
        </div>
      </div>
    </section>

    <section class="workspace card">
      <div class="workspace-tabs">
        <button :class="['tab-btn', activeTab === 'patient' ? 'active' : '']" @click="activeTab = 'patient'">
          患者 AI 处方
        </button>
        <button :class="['tab-btn', activeTab === 'doctor' ? 'active' : '']" @click="activeTab = 'doctor'">
          医生审核工作台
        </button>
      </div>

      <div v-if="activeTab === 'patient'" class="patient-grid">
        <div class="panel card chat-panel">
          <header>
            <div>
              <h3>AI 问诊面板</h3>
              <p class="muted">按照系统提示提供症状、病史、体征等信息</p>
            </div>
          </header>

          <form class="form-grid chat-meta-form" @submit.prevent>
            <label>
              挂号ID
              <input v-model="patientForm.registerId" placeholder="例如 10001" />
            </label>
            <label>
              患者ID
              <input v-model="patientForm.patientId" placeholder="例如 20001" />
            </label>
          </form>

          <div class="chat-history" ref="chatHistoryRef" @scroll="handleChatScroll">
            <p class="muted">会话记录</p>
                <div v-if="isConversationsLoading" class="muted">正在加载历史对话…</div>
            <div v-if="!chatHistory.length" class="empty">等待患者输入信息…</div>
            <ul v-else>
              <li v-for="(item, index) in chatHistory" :key="index" :class="item.role">
                <div class="meta">
                  <span>{{ item.role === 'user' ? '患者' : 'AI' }}</span>
                  <time>{{ formatTime(item.time) }}</time>
                </div>
                <div class="chat-content" v-html="renderMarkdown(item.content)"></div>
              </li>
            </ul>
            <div v-if="!shouldAutoScroll" class="scroll-bottom-wrapper">
              <button class="scroll-bottom-btn" type="button" @click="jumpToBottom">
                回到最新消息
              </button>
            </div>
          </div>

          <form class="form-grid chat-input-area" @submit.prevent="sendMessage">
            <label class="full">
              症状及补充信息
              <textarea
                v-model="patientForm.msg"
                rows="4"
                placeholder="描述症状细节、持续时间、既往病史、用药情况、伴随症状等"
                @keydown.enter.exact.prevent="handleEnterSend"
              ></textarea>
            </label>
            <div class="full form-actions">
              <button class="btn btn-primary" type="submit" :disabled="isChatLoading">
                {{ isChatLoading ? '正在诊断…' : '提交给 AI 诊断' }}
              </button>
            </div>
          </form>
        </div>

      </div>

      <div v-else class="doctor-grid">
        <div class="panel card">
          <header>
            <div style="display:flex; flex-direction:column; gap:8px;">
              <div style="display:flex; align-items:center; justify-content:space-between; gap:12px;">
                <div>
                  <h3>待审核处方列表</h3>
                  <p class="muted">点击条目可在弹窗中查看处方详情并进行审核</p>
                </div>
                <div style="display:flex; gap:12px; align-items:center;">
                  <input v-model="searchQuery" placeholder="搜索：处方ID / 患者ID / 挂号ID / 诊断关键词" class="search-input" />
                  <button class="btn btn-secondary" @click="fetchList">刷新</button>
                </div>
              </div>
            </div>
          </header>

          <div v-if="isListLoading" class="muted">正在加载处方列表…</div>
          <div v-else>
            <div v-if="!prescriptions.length" class="empty">当前没有处方</div>
            <ul v-else class="prescription-list">
              <li v-for="pres in filteredList" :key="pres.prescriptionId" class="pres-item" @click="openPrescription(pres.prescriptionId)">
                <div class="pres-left">
                  <div class="pres-icon">Rx</div>
                </div>
                <div class="pres-body">
                  <div class="pres-title-row">
                    <div class="pres-title">处方 #{{ pres.prescriptionId }}</div>
                    <div class="pres-badges">
                      <span v-if="pres.status == 0" class="badge badge-warning">未审核</span>
                      <span v-else class="badge" style="background: rgba(16,185,129,0.08); color:#047857; border:1px solid rgba(16,185,129,0.12)">已审核</span>
                    </div>
                  </div>
                  <div class="pres-meta">
                    <span class="meta-item">患者ID: <strong>{{ pres.patientId || '-' }}</strong></span>
                    <span class="meta-item">挂号ID: <strong>{{ pres.registerId || '-' }}</strong></span>
                  </div>
                  <div class="pres-snippet muted">{{ pres.diagnosis ? (pres.diagnosis.length > 120 ? pres.diagnosis.slice(0,120)+'...' : pres.diagnosis) : '无诊断摘要' }}</div>
                </div>
                <div class="pres-right">
                  <div style="display:flex; flex-direction:column; gap:8px; align-items:center; justify-content:center; height:100%;">
                    <button class="btn btn-primary btn-sm view-btn" @click.stop="openPrescription(pres.prescriptionId)">查看</button>
                    <div class="pres-time">{{ formatTime(pres.createTime || pres.create_time || new Date()) }}</div>
                  </div>
                </div>
              </li>
            </ul>
            <!-- 分页控件 -->
            <div class="pagination" style="display:flex; justify-content:center; align-items:center; gap:12px; margin-top:12px;">
              <button class="btn btn-secondary" :disabled="currentPage<=1" @click="prevPage">上一页</button>
              <span class="muted">第 {{ currentPage }} / {{ Math.max(1, Math.ceil((totalCount || 0) / pageSize)) }} 页（共 {{ totalCount }} 条）</span>
              <button class="btn btn-secondary" :disabled="currentPage>=Math.max(1, Math.ceil((totalCount || 0) / pageSize))" @click="nextPage">下一页</button>
            </div>
          </div>
        </div>
      </div>

      <!-- 模态弹窗（插入在此处，和页面通知同级） -->
      <teleport to="body">
        <transition name="fade">
          <div v-if="showReviewModal" class="modal-overlay" @click.self="closeReviewModal">
            <div class="modal-window card" role="dialog" aria-modal="true">
              <header class="modal-header">
                <h3>医生审核工作台</h3>
                <button class="link danger" @click="closeReviewModal">关闭</button>
              </header>

              <div class="modal-body">
                <div class="panel">
                  <header>
                    <div>
                      <h4>处方详情</h4>
                      <p class="muted">正在查看所选处方，点击“刷新”可重新加载当前处方数据</p>
                    </div>
                    <button class="btn btn-secondary" @click="refreshReview">刷新</button>
                  </header>
                  <div v-if="!reviewData" class="empty">等待输入处方ID…</div>
                  <div v-else class="review-summary">
                    <h4>处方概览</h4>
                    <ul>
                      <li>
                        <span>患者ID</span>
                        <strong>{{ reviewForm.prescription?.patientId }}</strong>
                      </li>
                      <li>
                        <span>挂号ID</span>
                        <strong>{{ reviewForm.prescription?.registerId }}</strong>
                      </li>
                      <li>
                        <span>诊断</span>
                        <strong>{{ reviewForm.prescription?.diagnosis }}</strong>
                      </li>
                    </ul>
                  </div>
                </div>

                <div class="panel" v-if="reviewData" style="margin-top: 12px;">
                  <header>
                    <div>
                      <h4>药品明细与审核</h4>
                      <p class="muted">可对 AI 推荐药品进行剂量、理由调整</p>
                    </div>
                    <button class="btn btn-secondary" @click="addDrugRow">新增药品</button>
                  </header>

                  <div class="drug-table-wrapper">
                    <table class="drug-table">
                      <thead>
                      <tr>
                        <th>药品ID</th>
                        <th>剂量</th>
                        <th>理由/备注</th>
                        <th>操作</th>
                      </tr>
                      </thead>
                      <tbody>
                      <tr v-for="(drug, index) in reviewForm.prescriptionDrugs" :key="drug.id || index">
                        <td>
                          <input v-model="drug.drugId" placeholder="药品ID" />
                        </td>
                        <td>
                          <input v-model="drug.dosage" placeholder="剂量" />
                        </td>
                        <td>
                          <textarea v-model="drug.reason" rows="2" placeholder="理由说明"></textarea>
                        </td>
                        <td>
                          <button class="link danger" type="button" @click="removeDrug(index)">移除</button>
                        </td>
                      </tr>
                      </tbody>
                    </table>
                  </div>

                  <div class="form-actions" style="margin-top: 12px;">
                    <button class="btn btn-primary" :disabled="isSubmitLoading" @click="submitReview">
                      {{ isSubmitLoading ? '提交中…' : '提交审核结果' }}
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </transition>
      </teleport>
    </section>

    <transition name="fade">
      <div v-if="notification.show" class="toast" :class="notification.type">
        {{ notification.message }}
      </div>
    </transition>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onBeforeUnmount, onMounted, nextTick, watch } from 'vue'
import { marked } from 'marked'

const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8081'

const activeTab = ref('patient')
const patientForm = reactive({
  msg: '',
  registerId: '10001',
  patientId: '20001'
})
const chatHistory = ref([])
const chatHistoryRef = ref(null)
const shouldAutoScroll = ref(true)
const diagnosisResult = ref(null)
const isChatLoading = ref(false)
const isConversationsLoading = ref(false)
const lastLoadedIds = reactive({ registerId: null, patientId: null })
const notification = reactive({
  show: false,
  type: 'success',
  message: ''
})
const typingTimers = new Set()

marked.setOptions({
  breaks: true
})

const reviewData = ref(null)
const reviewForm = reactive({
  prescription: null,
  prescriptionDrugs: []
})
const isReviewLoading = ref(false)
const isSubmitLoading = ref(false)
const showReviewModal = ref(false)
const prescriptions = ref([])
const isListLoading = ref(false)
const searchQuery = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const totalCount = ref(0)
const filteredList = computed(() => {
  const q = String(searchQuery.value || '').trim().toLowerCase()
  if (!q) return prescriptions.value
  return prescriptions.value.filter((p) => {
    const pid = p.patientId ? String(p.patientId) : ''
    const rid = p.registerId ? String(p.registerId) : ''
    const presId = p.prescriptionId ? String(p.prescriptionId) : ''
    const diag = p.diagnosis ? String(p.diagnosis).toLowerCase() : ''
    return presId.includes(q) || pid.includes(q) || rid.includes(q) || diag.includes(q)
  })
})

const openReviewModal = async () => {
  showReviewModal.value = true
  await nextTick()
}

const closeReviewModal = () => {
  showReviewModal.value = false
}

// Esc 关闭支持 & 锁定 body 滚动
const onKeydown = (e) => {
  if (e.key === 'Escape' && showReviewModal.value) {
    closeReviewModal()
  }
}

watch(showReviewModal, (val) => {
  document.body.style.overflow = val ? 'hidden' : ''
})

onMounted(() => {
  window.addEventListener('keydown', onKeydown)
  // 页面加载时尝试读取历史对话（若已填写挂号ID或患者ID）
  if (patientForm.registerId || patientForm.patientId) {
    // 不阻塞加载页面 UI
    loadConversations().catch((e) => console.error('初始加载历史对话失败', e))
  }
})

// 监听 ID 变化，自动加载历史对话
watch([
  () => patientForm.registerId,
  () => patientForm.patientId
], ([newReg, newPid], [oldReg, oldPid]) => {
  const reg = newReg ? String(newReg).trim() : null
  const pid = newPid ? String(newPid).trim() : null
  if ((reg || pid) && (lastLoadedIds.registerId !== reg || lastLoadedIds.patientId !== pid)) {
    // 小的节流，避免短时间重复触发
    setTimeout(() => {
      loadConversations().catch((e) => console.error('watch loadConversations error', e))
    }, 120)
  }
})

// 当切换到医生面板时加载待审核列表
watch(activeTab, (val) => {
  if (val === 'doctor') {
    fetchList().catch((e) => console.error('fetchList error', e))
  }
})

const loadConversations = async (page = 1, size = 200) => {
  const reg = patientForm.registerId ? String(patientForm.registerId).trim() : null
  const pid = patientForm.patientId ? String(patientForm.patientId).trim() : null
  // 如果没有任何标识，跳过加载
  if (!reg && !pid) return
  // 如果已加载过相同 ID 则跳过
  if (lastLoadedIds.registerId === reg && lastLoadedIds.patientId === pid) return

  isConversationsLoading.value = true
  try {
    const params = new URLSearchParams()
    if (reg) params.set('registerId', reg)
    if (pid) params.set('patientId', pid)
    params.set('page', String(page))
    params.set('size', String(size))

    const res = await fetch(`${API_BASE}/prescription/conversations?${params.toString()}`)
    if (!res.ok) throw new Error('无法获取历史对话')
    const data = await res.json()
    console.debug('loadConversations response:', data)
    const records = Array.isArray(data.records) ? data.records : []
    // 后端按时间倒序返回，前端展示为从旧到新
    const ordered = records.slice().reverse()
    const mapped = ordered.map((r) => ({
      role: (r.type && r.type.toLowerCase() === 'user') ? 'user' : 'ai',
      content: r.content,
      time: r.timestamp || r.createTime || new Date()
    }))
    // 用历史记录替换当前聊天记录（保留当前会话中已存在的 AI 响应）
    chatHistory.value = mapped
    if (mapped.length === 0) {
      showToast('info', '未找到历史对话')
    } else {
      showToast('success', `已加载历史记录 ${mapped.length} 条`)
    }
    lastLoadedIds.registerId = reg
    lastLoadedIds.patientId = pid
    shouldAutoScroll.value = true
    await nextTick()
    scrollChatToBottom(true)
  } catch (e) {
    console.error('loadConversations error', e)
    showToast('error', e.message || '加载历史对话失败')
  } finally {
    isConversationsLoading.value = false
  }
}

const sendMessage = async () => {
  // 在发送前确保历史对话已加载（若 ID 变化或未加载）
  const currentReg = patientForm.registerId ? String(patientForm.registerId).trim() : null
  const currentPid = patientForm.patientId ? String(patientForm.patientId).trim() : null
  if ((currentReg || currentPid) && (lastLoadedIds.registerId !== currentReg || lastLoadedIds.patientId !== currentPid)) {
    await loadConversations()
  }

  const message = patientForm.msg.trim()
  if (!message) {
    return showToast('error', '请先填写症状及补充信息')
  }
  isChatLoading.value = true
  const payload = new URLSearchParams({
    msg: message,
    registerId: patientForm.registerId || 'default',
    patientId: patientForm.patientId || 'default'
  })
  chatHistory.value.push({
    role: 'user',
    content: message,
    time: new Date()
  })
  shouldAutoScroll.value = true
  scrollChatToBottom(true)
  patientForm.msg = ''
  try {
    const response = await fetch(`${API_BASE}/prescription/diagnosis?${payload.toString()}`, {
      method: 'POST'
    })
    const data = await response.json()
    if (!data.success) {
      throw new Error(data.message || 'AI 诊断失败')
    }
    diagnosisResult.value = data
    const aiEntry = reactive({
      role: 'ai',
      content: '',
      time: new Date()
    })
    chatHistory.value.push(aiEntry)
    scrollChatToBottom()
    typeWriter(data.diagnosis, (partial) => {
      aiEntry.content = partial
      scrollChatToBottom()
    })
    showToast('success', 'AI 已回复')
  } catch (error) {
    showToast('error', error.message)
    chatHistory.value.pop()
  } finally {
    isChatLoading.value = false
  }
}

// `resetChat` 已移除 — 使用页面刷新或手动清空输入替代

const loadReview = async (id) => {
  const targetId = id || reviewForm.prescription?.prescriptionId
  if (!targetId) {
    return showToast('error', '缺少处方ID，无法加载处方')
  }
  isReviewLoading.value = true
  try {
    const response = await fetch(`${API_BASE}/prescription/review/${targetId}`)
    if (!response.ok) {
      throw new Error('未找到对应处方')
    }
    const data = await response.json()
    reviewData.value = data
    reviewForm.prescription = { ...data.prescription, status: data.prescription?.status ?? 0 }
    reviewForm.prescriptionDrugs = (data.prescriptionDrugs || []).map((item) => ({ ...item }))
    showToast('success', '处方数据加载成功')
  } catch (error) {
    showToast('error', error.message)
    reviewData.value = null
  } finally {
    isReviewLoading.value = false
  }
}

const fetchList = async (page, size) => {
  // 确保 page 和 size 是有效的数字，如果接收到事件对象则使用默认值
  const pageNum = typeof page === 'number' ? page : currentPage.value
  const pageSize_ = typeof size === 'number' ? size : pageSize.value
  
  isListLoading.value = true
  try {
    const res = await fetch(`${API_BASE}/prescription/list?page=${pageNum}&size=${pageSize_}`)
    if (!res.ok) throw new Error('无法获取处方列表')
    const data = await res.json()
    prescriptions.value = Array.isArray(data.records) ? data.records : []
    totalCount.value = data.total || 0
    currentPage.value = data.page || pageNum
  } catch (e) {
    console.error('fetchList error', e)
    showToast('error', e.message || '加载处方列表失败')
  } finally {
    isListLoading.value = false
  }
}

const prevPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--
    fetchList()
  }
}

const nextPage = () => {
  const totalPages = Math.max(1, Math.ceil((totalCount.value || 0) / pageSize.value))
  if (currentPage.value < totalPages) {
    currentPage.value++
    fetchList()
  }
}

const openPrescription = async (prescriptionId) => {
  showReviewModal.value = true
  await nextTick()
  await loadReview(prescriptionId)
}

const refreshReview = async () => {
  const id = reviewForm.prescription?.prescriptionId
  if (!id) {
    showToast('info', '请先选择处方进行查看')
    return
  }
  try {
    isReviewLoading.value = true
    await loadReview(id)
  } finally {
    isReviewLoading.value = false
  }
}

const addDrugRow = () => {
  reviewForm.prescriptionDrugs.push({
    drugId: '',
    dosage: '',
    reason: ''
  })
}

const removeDrug = (index) => {
  reviewForm.prescriptionDrugs.splice(index, 1)
}

const submitReview = async () => {
  if (!reviewForm.prescription) {
    return showToast('error', '请先加载处方数据')
  }
  isSubmitLoading.value = true
  try {
    const payload = {
      prescription: {
        ...reviewForm.prescription,
        status: 1
      },
      prescriptionDrugs: reviewForm.prescriptionDrugs
    }
    const response = await fetch(`${API_BASE}/prescription/submitReview`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    })
    const data = await response.json()
    if (!data.success) {
      throw new Error(data.message || '审核提交失败')
    }
    showToast('success', '审核已提交，状态更新为已审核')
    // 提交成功后刷新处方列表
    await fetchList(currentPage.value, pageSize.value)
    // 关闭模态框
    closeReviewModal()
    // 清空审核表单
    reviewData.value = null
    reviewForm.prescription = null
    reviewForm.prescriptionDrugs = []
  } catch (error) {
    showToast('error', error.message)
  } finally {
    isSubmitLoading.value = false
  }
}

const showToast = (type, message) => {
  notification.type = type
  notification.message = message
  notification.show = true
  setTimeout(() => {
    notification.show = false
  }, 2600)
}

const formatTime = (time) => {
  const date = new Date(time)
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

const renderMarkdown = (text) => {
  if (!text) return ''
  return marked.parse(text)
}

const scrollChatToBottom = (force = false) => {
  if (!force && !shouldAutoScroll.value) return
  nextTick(() => {
    const el = chatHistoryRef.value
    if (el) {
      el.scrollTop = el.scrollHeight
    }
  })
}

const handleChatScroll = () => {
  const el = chatHistoryRef.value
  if (!el) return
  const nearBottom = el.scrollTop + el.clientHeight >= el.scrollHeight - 20
  shouldAutoScroll.value = nearBottom
}

const jumpToBottom = () => {
  shouldAutoScroll.value = true
  scrollChatToBottom(true)
}

const handleEnterSend = () => {
  if (isChatLoading.value) return
  sendMessage()
}

const typeWriter = (text, onUpdate, speed = 20) => {
  let index = 0
  const timer = setInterval(() => {
    index++
    onUpdate(text.slice(0, index))
    if (index >= text.length) {
      clearInterval(timer)
      typingTimers.delete(timer)
    }
  }, speed)
  typingTimers.add(timer)
}

onBeforeUnmount(() => {
  typingTimers.forEach((timer) => clearInterval(timer))
  typingTimers.clear()
  document.body.style.overflow = ''
  window.removeEventListener('keydown', onKeydown)
})
</script>

<style scoped>
.home-page {
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.hero {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 32px;
  padding: 40px 48px;
  position: relative;
  overflow: hidden;
}

.hero-content h1 {
  font-size: 42px;
  margin: 18px 0 12px;
  background: var(--gradient-primary);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.hero-content p {
  color: var(--light-text);
  max-width: 520px;
}

.hero-actions {
  display: flex;
  gap: 16px;
  margin: 24px 0 32px;
  flex-wrap: wrap;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  padding: 16px 20px;
  border-radius: 16px;
}

.hero-stats h3 {
  font-size: 28px;
  margin: 0;
}

.hero-visual {
  position: relative;
  min-height: 320px;
}

.pulse-ring {
  position: absolute;
  inset: 0;
  border-radius: 32px;
  border: 1.5px dashed rgba(14, 165, 233, 0.5);
  animation: pulse 3.2s ease-in-out infinite;
}

.hero-chat-preview {
  position: relative;
  padding: 24px;
  margin-top: 24px;
  backdrop-filter: blur(10px);
}

.hero-chat-preview ul {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 16px;
}

.status-section {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 20px;
}

.status-card {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.status-card h3 {
  font-size: 30px;
  margin: 4px 0;
}

.workspace {
  padding: 32px;
}

.workspace-tabs {
  display: flex;
  gap: 16px;
  border-bottom: 1px solid rgba(14, 165, 233, 0.15);
  margin-bottom: 24px;
}

.tab-btn {
  background: transparent;
  border: none;
  padding: 12px 20px;
  font-weight: 600;
  cursor: pointer;
  color: var(--light-text);
  border-bottom: 3px solid transparent;
  transition: all 0.2s ease;
}

.tab-btn.active {
  color: var(--primary-blue);
  border-color: var(--primary-blue);
}

.patient-grid {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.doctor-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 24px;
}

.panel {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.chat-panel {
  min-height: 420px;
}

.diagnosis-panel {
  min-height: 360px;
}

.panel header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.muted {
  color: var(--light-text);
  font-size: 14px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
}

.form-grid label {
  display: flex;
  flex-direction: column;
  font-weight: 600;
  font-size: 14px;
  color: var(--dark-text);
  gap: 6px;
}

.form-grid input,
.form-grid textarea,
.review-form input,
.drug-table input,
.drug-table textarea {
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px solid rgba(14, 165, 233, 0.25);
  background: rgba(255, 255, 255, 0.9);
  font-size: 14px;
  resize: none;
}

.form-grid .full {
  grid-column: 1 / -1;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
}

.chat-input-area {
  border-top: 1px solid rgba(14, 165, 233, 0.12);
  padding-top: 16px;
  margin-top: 8px;
}

.chat-meta-form {
  border-bottom: 1px dashed rgba(14, 165, 233, 0.15);
  padding-bottom: 12px;
  margin-bottom: 12px;
}

.chat-history {
  max-height: 420px;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
  position: relative;
}

.scroll-bottom-wrapper {
  position: sticky;
  bottom: 0;
  display: flex;
  justify-content: center;
  padding: 12px 0 4px;
}

.scroll-bottom-btn {
  padding: 6px 20px;
  border-radius: 999px;
  border: none;
  background: var(--gradient-primary);
  color: #fff;
  box-shadow: var(--shadow-md);
  cursor: pointer;
  font-size: 13px;
}

.scroll-bottom-btn:hover {
  box-shadow: var(--shadow-lg);
}

.chat-history ul {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.chat-history li {
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(14, 165, 233, 0.05);
  border: 1px solid rgba(14, 165, 233, 0.12);
}

.chat-history li.ai {
  background: rgba(16, 185, 129, 0.06);
  border-color: rgba(16, 185, 129, 0.15);
}

.prescription-list {
  list-style: none;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 12px;
}
.pres-item {
  padding: 12px;
  border-radius: 10px;
  border: 1px solid rgba(14,165,233,0.12);
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.pres-item:hover { background: rgba(14,165,233,0.03); }
.pres-meta { display:flex; gap:12px; align-items:center; font-size:14px }
.pres-snippet { font-size:13px }

/* Enhanced prescription card styles */
.pres-item {
  padding: 20px 18px;
  border-radius: 12px;
  border: 1px solid rgba(14,165,233,0.08);
  display: flex;
  gap: 16px;
  align-items: flex-start;
  min-height: 88px;
  transition: transform .14s ease, box-shadow .14s ease, background .14s ease;
}
.pres-item:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 32px rgba(14,165,233,0.07);
}
.pres-left { width:72px; display:flex; align-items:flex-start; justify-content:center }
.pres-icon {
  width:56px; height:56px; border-radius:12px; display:flex; align-items:center; justify-content:center;
  background: linear-gradient(135deg, rgba(14,165,233,0.12), rgba(99,102,241,0.08));
  color: var(--primary-blue, #0ea5e9); font-weight:800; font-size:16px;
  align-self:flex-start;
}
.pres-body { flex:1; display:flex; flex-direction:column; gap:8px; text-align:left }
.pres-title-row { display:flex; justify-content:space-between; align-items:flex-start }
.pres-title { font-size:16px; font-weight:700 }
.pres-badges { display:flex; gap:8px; align-items:center }
.badge { padding:6px 10px; border-radius:999px; font-size:12px; }
.badge-warning { background: rgba(250,204,21,0.12); color: #c27803; border: 1px solid rgba(250,204,21,0.18) }
.pres-right { width:140px; display:flex; flex-direction:column; gap:10px; align-items:flex-end; margin-left: auto; justify-content: center }
.pres-time { font-size:12px; color:var(--light-text); }
.btn-sm { padding:6px 10px; font-size:13px }
.view-btn { padding:10px 16px; font-size:14px; border-radius: 999px; box-shadow: 0 6px 18px rgba(14,165,233,0.12); }
.pres-snippet {
  font-size:14px;
  color: var(--dark-text);
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.search-input {
  padding: 8px 12px;
  border-radius: 10px;
  border: 1px solid rgba(14,165,233,0.12);
  min-width: 360px;
}

.chat-content {
  line-height: 1.6;
}

.chat-content p {
  margin: 0 0 6px;
}

.chat-content pre {
  background: rgba(15, 23, 42, 0.85);
  color: #f8fafc;
  padding: 10px 12px;
  border-radius: 12px;
  font-size: 13px;
  overflow-x: auto;
}

.chat-content code {
  background: rgba(14, 165, 233, 0.12);
  padding: 2px 4px;
  border-radius: 6px;
  font-size: 13px;
}

.chat-content ul {
  margin: 4px 0 4px 18px;
  padding: 0;
}

.chat-history .meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--light-text);
  margin-bottom: 6px;
}

.doctor-grid .panel {
  min-height: 320px;
}

.review-form {
  display: flex;
  gap: 16px;
  align-items: flex-end;
}

.review-form label {
  flex: 1;
  font-weight: 600;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.review-summary ul {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.review-summary li {
  display: flex;
  justify-content: space-between;
  font-size: 15px;
}

.drug-table-wrapper {
  overflow-x: auto;
}

.drug-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0 12px;
}

.drug-table th {
  text-align: left;
  font-size: 13px;
  color: var(--light-text);
  padding-bottom: 4px;
}

.drug-table td {
  vertical-align: top;
}

.drug-table textarea {
  min-width: 200px;
}

.link {
  background: none;
  border: none;
  cursor: pointer;
  font-weight: 600;
}

.link.danger {
  color: var(--warning-red);
}

.timeline-steps {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 24px;
  margin-top: 20px;
}

.timeline-step {
  padding: 20px;
  border-radius: 16px;
  border: 1px dashed rgba(14, 165, 233, 0.2);
  background: rgba(255, 255, 255, 0.85);
}

.toast {
  position: fixed;
  right: 32px;
  bottom: 32px;
  padding: 14px 24px;
  border-radius: 14px;
  color: #fff;
  box-shadow: var(--shadow-md);
}

.toast.success {
  background: var(--gradient-primary);
}

.toast.error {
  background: var(--warning-red);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.empty,
.placeholder {
  padding: 20px;
  border: 1px dashed rgba(14, 165, 233, 0.2);
  border-radius: 16px;
  text-align: center;
  color: var(--light-text);
}

/* 弹窗样式 */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(2,6,23,0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 99999;
  padding: 20px;
}
.modal-window {
  width: min(1100px, 98%);
  max-height: 90vh;
  overflow: auto;
  padding: 18px;
  border-radius: 12px;
  background: rgba(255,255,255,0.98);
  box-shadow: var(--shadow-lg);
}
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}
.modal-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

@media (max-width: 768px) {
  .hero {
    padding: 32px 24px;
  }
  .hero-stats {
    grid-template-columns: repeat(2, 1fr);
  }
  .review-form {
    flex-direction: column;
    align-items: stretch;
  }
  .form-actions {
    justify-content: center;
  }
}
</style>
