<script def>
{
  "navigationBarTitleText": "菜谱确认"
}
</script>

<script setup>
export default {
  data: {
    dishName: '番茄炒蛋',
    servings: 2,
    estimatedTimeMinutes: 15,
    ingredient1: '番茄  2个 / 切块',
    ingredient2: '鸡蛋  3个 / 打散',
    step1: '1  炒鸡蛋',
    step2: '2  炒番茄'
  },
  confirmRecipe() {
    wx.navigateTo({
      url: '/pages/cooking-guide/index'
    });
  }
}
</script>

<page>
  <view class="page">
    <text class="dish-name">{{dishName}}</text>
    <text class="meta">{{servings}}人份 · 约{{estimatedTimeMinutes}}分钟</text>

    <view class="card">
      <text class="card-title">食材</text>
      <text class="line">{{ingredient1}}</text>
      <text class="line">{{ingredient2}}</text>
    </view>

    <view class="card">
      <text class="card-title">步骤</text>
      <text class="line">{{step1}}</text>
      <text class="line">{{step2}}</text>
    </view>

    <button class="btn-start" bindtap="confirmRecipe">开始做菜</button>
  </view>
</page>

<style>
.page {
  display: flex;
  flex-direction: column;
  padding: 16px;
  gap: 10px;
  background: #000000;
  color: #ffffff;
}

.dish-name {
  color: #40FF5E;
  font-size: 22px;
  font-weight: 700;
  line-height: 28px;
}

.meta {
  font-size: 12px;
  color: #888888;
  line-height: 16px;
}

.card {
  padding: 10px 14px;
  border-radius: 10px;
  background: #1a1a1a;
  border: 1px solid #333333;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.card-title {
  font-size: 12px;
  font-weight: 600;
  color: #888888;
}

.line {
  font-size: 14px;
  line-height: 22px;
  color: #ffffff;
}

.btn-start {
  color: #000000;
  background: #40FF5E;
  border: 2px solid #40FF5E;
  border-radius: 12px;
  box-sizing: border-box;
  padding: 10px;
  line-height: 24px;
  text-align: center;
  font-size: 15px;
  font-weight: 700;
}
</style>
