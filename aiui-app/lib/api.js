// 见味 API 模块
// 注意：为避免调试器中的网络问题，各页面使用硬编码的自包含数据。
// 本模块保留为示例，可按需扩展为真实 API 调用。

export function getRecipeDraft() {
  return Promise.resolve({
    recipeId: 'mock-recipe-001',
    dishName: '番茄炒蛋',
    servings: 2,
    estimatedTimeMinutes: 15,
    ingredients: [
      { name: '番茄', amount: '2 个', prep: '切块' },
      { name: '鸡蛋', amount: '3 个', prep: '打散' }
    ],
    steps: [
      {
        id: 'step-01',
        title: '炒鸡蛋',
        instruction: '鸡蛋液下锅，炒到半凝固后盛出。',
        heat: '中火',
        targetState: '鸡蛋半凝固，表面略湿润。',
        checkable: true
      },
      {
        id: 'step-02',
        title: '炒番茄',
        instruction: '番茄下锅炒出汁，再加入鸡蛋翻炒。',
        heat: '中火',
        targetState: '番茄出汁，锅底有红色汤汁。',
        checkable: true
      }
    ]
  });
}
