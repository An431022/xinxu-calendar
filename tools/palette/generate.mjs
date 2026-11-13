// 用 Material 官方 material-color-utilities 计算 HCT 色值，
// 保证色板落在 Google Material 3 的色调体系内（低饱和、不刺眼）。
// 运行： node generate.mjs   →  palette.json
//
// HCT = Hue(色相) + Chroma(彩度) + Tone(感知明度)，Material 3 动态取色的底层色彩空间。
import { Hct, argbFromHex, hexFromArgb } from "@material/material-color-utilities";
import { writeFileSync } from "node:fs";

// ---- 三条硬约束（用来把「不刺眼」变成能写进代码的东西）----
//   chroma <= 48  M3 色调板的实际上限，再高就开始尖锐
//   tone 52..78   白底上看得清，又不至于压住日期数字
//   暖 5 / 冷 5 / 中性 2   冷暖统计要对半开才公平
const CHROMA_CAP = 48;

// lightOverride: 用户直接指定的色值。指定后以它为准，hue/chroma/tone 从它反算出来，
// 深色模式仍然按同一套规则（同 hue/chroma，tone + 15）推导，保持是「同一个颜色的两种明度」。
const SPEC = [
  // ---- 暖色 5 ----
  { id: "warm_rose",    name: "玫瑰", temp: "warm", h: 355, c: 44, tone: 68, meaning: "心动、柔软" },
  { id: "warm_flame",   name: "朱红", temp: "warm", h: 27,  c: 48, tone: 62, meaning: "热烈、激动、生气",
    // 用户 2026-10-03 指定：原 #E27768 被认为「有些素了」。这个色相自带强度，
    // 而「热烈、激动、生气」本来就该是整块色板里最响的那个，所以这里有意突破彩度上限。
    lightOverride: "#ed5126" },
  { id: "warm_honey",   name: "蜜橘", temp: "warm", h: 55,  c: 48, tone: 68, meaning: "充实、有干劲" },
  { id: "warm_amber",   name: "琥珀", temp: "warm", h: 88,  c: 44, tone: 70, meaning: "开心、轻快" },
  { id: "warm_caramel", name: "焦糖", temp: "warm", h: 42,  c: 28, tone: 52, meaning: "踏实、沉稳" },
  // ---- 冷色 5 ----
  { id: "cool_moss",    name: "苔绿", temp: "cool", h: 135, c: 28, tone: 58, meaning: "缓慢、满足" },
  { id: "cool_mint",    name: "薄荷", temp: "cool", h: 170, c: 34, tone: 68, meaning: "清爽、放松" },
  { id: "cool_sky",     name: "晴空", temp: "cool", h: 215, c: 40, tone: 68, meaning: "平静、清醒" },
  { id: "cool_deep",    name: "深海", temp: "cool", h: 255, c: 40, tone: 52, meaning: "低落、沉思" },
  { id: "cool_mist",    name: "雾紫", temp: "cool", h: 300, c: 30, tone: 68, meaning: "忧郁、敏感" },
  // ---- 中性 2（不参与冷暖统计）----
  { id: "neutral_cloud", name: "云灰", temp: "neutral", h: 250, c: 6, tone: 78, meaning: "平淡、记不清" },
  { id: "neutral_ink",   name: "石墨", temp: "neutral", h: 250, c: 8, tone: 44, meaning: "沉重、黑暗" },
];

const hex = (h, c, t) => hexFromArgb(Hct.from(h, c, t).toInt());

// 深色模式：M3 惯例是把 tone 抬高，保持同一 hue/chroma。
const darkTone = (t) => Math.min(88, t + 15);

// 每个颜色先定出权威的 (h, c, t)：有 lightOverride 就从它反算，否则用 SPEC 里写的。
const resolved = SPEC.map((s) => {
  if (s.lightOverride) {
    const hct = Hct.fromInt(argbFromHex(s.lightOverride));
    return { ...s, h: hct.hue, c: hct.chroma, t: hct.tone, light: hexFromArgb(hct.toInt()) };
  }
  return { ...s, t: s.tone, light: hex(s.h, s.c, s.tone) };
});

const colors = resolved.map((s) => ({
  id: s.id,
  name: s.name,
  temperature: s.temp,
  meaning: s.meaning,
  hct: { hue: +s.h.toFixed(1), chroma: +s.c.toFixed(1), tone: +s.t.toFixed(1) },
  light: s.light,
  dark: hex(s.h, s.c, darkTone(s.t)),
}));

// 每个色相的完整色调阶梯，方便对比"哪个明度最舒服"
const TONES = [20, 30, 40, 50, 60, 70, 80, 90];
const ladders = resolved
  .filter((s) => s.temp !== "neutral")
  .map((s) => ({
    name: s.name,
    hue: +s.h.toFixed(1),
    chroma: +s.c.toFixed(1),
    tones: TONES.map((t) => ({ tone: t, hex: hex(s.h, s.c, t) })),
  }));

writeFileSync(
  new URL("./palette.json", import.meta.url),
  JSON.stringify({ generatedBy: "@material/material-color-utilities", chromaCap: CHROMA_CAP, colors, ladders }, null, 2),
  "utf8"
);

console.log("12 colors:");
for (const c of colors) {
  const flag = c.hct.chroma > CHROMA_CAP ? "  <-- 超过彩度上限" : "";
  console.log(
    `  ${c.name.padEnd(4)} ${c.temperature.padEnd(8)} light=${c.light}  dark=${c.dark}   ` +
      `H${c.hct.hue} C${c.hct.chroma} T${c.hct.tone}${flag}`
  );
}

// 约束自检：把「有意破例」的地方显式打出来，不要让它悄悄溜过去。
console.log("\n约束自检:");
const overChroma = colors.filter((c) => c.hct.chroma > CHROMA_CAP);
const outOfTone = colors.filter((c) => c.hct.tone < 52 || c.hct.tone > 78);
console.log(
  overChroma.length
    ? `  彩度 > ${CHROMA_CAP}: ${overChroma.map((c) => `${c.name}(C${c.hct.chroma})`).join("、")}`
    : `  彩度全部 <= ${CHROMA_CAP}`
);
console.log(
  outOfTone.length
    ? `  明度出界 52..78: ${outOfTone.map((c) => `${c.name}(T${c.hct.tone})`).join("、")}`
    : "  明度全部落在 52..78"
);
const temps = colors.reduce((acc, c) => ((acc[c.temperature] = (acc[c.temperature] ?? 0) + 1), acc), {});
console.log(`  冷暖分布: 暖 ${temps.warm} / 冷 ${temps.cool} / 中性 ${temps.neutral}`);
