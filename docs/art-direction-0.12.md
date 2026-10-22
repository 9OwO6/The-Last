# 0.12 视觉方向：把生活收进小抽屉

统一角色是戴鼠尾草绿领巾的奶油小熊：短身、大头、圆耳、桃色腮红。采用水粉和色铅笔的软质绘本笔触，与纸白、燕麦、杏桃、草木绿的界面配色匹配。

## 场景与信息的对应

| 素材 | 使用位置 | 为什么放在这里 |
|---|---|---|
| `story_drawer.webp` | 首页主视觉 | 把心意收进抽屉，对应收集生活小事；与标题并排，不单独占一整屏 |
| `story_calendar.webp` | 日历页头 | 翻动心形日历页，对应回看某一天；日期格中不放插画，便于查看记录 |
| `story_garden.webp` | 统计页头 | 浇灌不同高度的小花，对应自己的生活节奏，不强调打卡成绩 |
| `story_delivery.webp` | 更新中心 | 递送包裹，对应接收新版本；下载、安装按钮独立于图案，操作明确 |
| `story_journal.webp` | 首次记录及空历史 | 等待动笔的空白手账，对应温和邀请开始记录；不重复放进已有记录的密集列表 |

所有素材存于 `app/src/main/res/drawable-nodpi/`。使用内置 imagegen 生成真实透明 PNG，再等比例缩小为最长边 800px 的透明 WebP。保留 alpha，不做背景替换或角色拼接。五张资源合计约 627 KiB，无额外运行依赖。

导航统一使用 Rounded 功能图标；事项图案仍由事项类型决定，不把所有事项都替换成小熊。卡片用纸色底、细边线与抽屉式不对称小圆角，信息和按钮保持优先可读。夜间模式使用相同角色及暗色纸张，不反转插画。

## 生成提示词

主视觉：

> Create ONE production illustration asset for an Android Chinese personal life-event journal app called The Last. No text, no letters, no UI mockup, no borders. Transparent background with real alpha. Landscape composition aspect about 3:2. A distinctive super adorable chibi butter-cream baby bear, tiny rounded ears, large head and compact plush body, tiny dark chocolate oval eyes, tiny nose, peach blush cheeks, wearing a small sage-green neckerchief, sits beside a little honey-colored wooden chest with two rounded drawers. The top drawer is open and contains a tiny blank notebook, a leaf and a small star; bear carefully tucks a small peach heart-shaped keepsake into the drawer. Emotional scene of keeping little moments. Beautiful professionally art-directed Japanese storybook gouache illustration, soft painterly colored-pencil edges, subtly textured matte colors, believable cute anatomy, sparse tiny glints only, gentle warm lighting, not glossy 3D, not clipart or vector. Cream/oatmeal fur, muted sage, apricot and butter yellow, warm chocolate contours. All objects and bear fully within image with generous transparent margins; overall scene compact, easy to read at 160dp wide. The bear and chest have equal prominence, objects at lower part of scene. This will be the signature hero illustration of a cohesive cute journal design system. Save the generated image asset to a local file.

其余四张均以主视觉作为角色和画风参考，不修改原图。共用前缀：

> Create ONE separate production illustration asset for the same life-event journal app. The attached image is STYLE AND CHARACTER REFERENCE ONLY, not an edit target. Preserve this exact adorable cream chibi baby bear design: big round head, tiny round ears, chocolate oval eyes, peach blush, sage neckerchief, plush stubby body. Match its soft warm hand-painted gouache and colored-pencil storybook texture, muted cream, sage, apricot, honey palette. Real transparent alpha background, NO text, letters, numbers, border, scene backdrop, gradients or glow outside the objects. Compact isolated vignette, fully visible subject with 10% transparent padding, easily readable at 100dp.

分别追加：

- 日历：The bear sits beside an oversized small desktop flip calendar made of cream paper with a sage base and honey binding rings. The calendar page has only a simple peach heart icon, no numbers. Bear holds one page gently as though remembering a day. One little dried leaf at the base. Calm reflective pose. Landscape 3:2.
- 统计：The bear is a tiny gardener, holding a little apricot watering can and carefully watering exactly three small pastel flowers growing from a rounded sage ceramic planter. Flowers of differing heights symbolize a person's natural life rhythm, not achievement. Bear looks pleased but peaceful. Small water drops. Landscape 3:2.
- 更新：The same bear is a little courier hugging a small peach parcel with a sage ribbon. A small blank cream envelope with a heart seal is tucked under the ribbon. Bear stands with one paw waving hello, warm welcoming face. No extra flora. Square composition, clear silhouette.
- 空状态：The same bear sits with a large open blank cream journal resting on its lap and a tiny honey pencil in its paw, looking expectantly at the viewer. Pages blank with one tiny peach heart, no writing. A small apricot cushion underneath. Calm encouraging smile. Square composition.
