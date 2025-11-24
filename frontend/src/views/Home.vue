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
            <button class="btn btn-secondary" @click="resetChat">重置会话</button>
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
            <div>
              <h3>处方审核</h3>
            </div>
            <button class="btn btn-secondary" @click="refreshReview">刷新</button>
          </header>

          <div class="muted">点击下面按钮在弹窗中打开完整审核界面</div>
          <div class="form-actions" style="margin-top: 12px;">
            <button class="btn btn-primary" @click="openReviewModal">打开审核弹窗</button>
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
                      <h4>处方审核检索</h4>
                      <p class="muted">根据处方编号加载 AI 提交的处方数据</p>
                    </div>
                    <button class="btn btn-secondary" @click="refreshReview">刷新</button>
                  </header>
                  <form class="review-form" @submit.prevent="loadReview">
                    <label>
                      处方ID
                      <input ref="reviewIdInputRef" v-model="reviewIdInput" placeholder="请输入处方ID" />
                    </label>
                    <button class="btn btn-primary" type="submit" :disabled="isReviewLoading">
                      {{ isReviewLoading ? '加载中…' : '获取处方' }}
                    </button>
                  </form>
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
import { reactive, ref, onBeforeUnmount, onMounted, nextTick, watch } from 'vue'
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
const notification = reactive({
  show: false,
  type: 'success',
  message: ''
})
const typingTimers = new Set()

marked.setOptions({
  breaks: true
})

const overviewCards = [
  { title: '今日问诊', value: '32', sub: 'AI 初诊已完成', trend: '+12%' },
  { title: '待审核处方', value: '6', sub: '需医生确认', trend: '-2' },
  { title: '药品相互作用报警', value: '1', sub: '已推送提醒', trend: '+1' },
  { title: '医生在线', value: '4', sub: '审核团队', trend: '+1' }
]

const reviewIdInput = ref('')
const reviewData = ref(null)
const reviewForm = reactive({
  prescription: null,
  prescriptionDrugs: []
})
const isReviewLoading = ref(false)
const isSubmitLoading = ref(false)
const showReviewModal = ref(false)
const reviewIdInputRef = ref(null)

const openReviewModal = async () => {
  showReviewModal.value = true
  await nextTick()
  if (reviewIdInputRef.value) reviewIdInputRef.value.focus()
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
})

const sendMessage = async () => {
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

const resetChat = () => {
  chatHistory.value = []
  diagnosisResult.value = null
  patientForm.msg = ''
  shouldAutoScroll.value = true
}

const loadReview = async () => {
  if (!reviewIdInput.value) {
    return showToast('error', '请输入处方ID')
  }
  isReviewLoading.value = true
  try {
    const response = await fetch(`${API_BASE}/prescription/review/${reviewIdInput.value}`)
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

const refreshReview = () => {
  if (reviewIdInput.value) {
    loadReview()
  } else {
    reviewData.value = null
    reviewForm.prescription = null
    reviewForm.prescriptionDrugs = []
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
