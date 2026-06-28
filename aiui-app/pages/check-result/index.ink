<script def>
{
  "navigationBarTitleText": "检查结果"
}
</script>

<script setup>
import wx from 'wx';

export default {
  data: {
    status: 'pass',
    statusText: '通过',
    summary: '',
    suggestion: '',
    badgeClass: 'badge badge-pass'
  },

  onLoad(query) {
    var status = decodeURIComponent(query.status || 'pass');
    var summary = decodeURIComponent(query.summary || '');
    var suggestion = decodeURIComponent(query.suggestion || '');
    var tts = decodeURIComponent(query.tts || '');

    var statusText = '通过';
    var badgeClass = 'badge badge-pass';
    if (status === 'continue') {
      statusText = '继续';
      badgeClass = 'badge badge-continue';
    } else if (status === 'adjust') {
      statusText = '需调整';
      badgeClass = 'badge badge badge-adjust';
    }

    this.setData({
      status: status,
      statusText: statusText,
      summary: summary,
      suggestion: suggestion,
      badgeClass: badgeClass
    });

    if (tts) {
      wx.speech.playTTS(tts);
    }
  },

  goBack() {
    wx.navigateBack({ delta: 2 });
  }
}
</script>

<page>
  <view class="page">
    <view class="{{badgeClass}}">
      <text class="badge-text">{{statusText}}</text>
    </view>

    <view class="card">
      <text class="summary">{{summary}}</text>
      <text class="suggestion">{{suggestion}}</text>
    </view>

    <view class="action-row">
      <button bindtap="goBack">返回步骤</button>
    </view>
  </view>
</page>

<style>
.page {
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  background: #000000;
  color: #ffffff;
}

.badge {
  padding: 10px;
  border-radius: 10px;
  text-align: center;
}

.badge-pass {
  background: #40FF5E;
}

.badge-continue {
  background: #FFA500;
}

.badge-adjust {
  background: #FF4444;
}

.badge-text {
  font-size: 18px;
  font-weight: 700;
  color: #000000;
}

.card {
  flex: 1;
  padding: 12px 14px;
  border-radius: 10px;
  background: #1a1a1a;
  border: 1px solid #333333;
  display: flex;
  flex-direction: column;
  gap: 6px;
  justify-content: center;
}

.summary {
  font-size: 16px;
  font-weight: 600;
  color: #ffffff;
}

.suggestion {
  font-size: 13px;
  color: #888888;
}

.action-row {
  display: flex;
  gap: 10px;
}

.action-row button {
  flex: 1;
  background: #1a1a1a;
  color: #ffffff;
  border: 1px solid #333333;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
}
</style>
