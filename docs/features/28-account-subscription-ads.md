# 28 — Account, Subscription, Credits & Ads

> **Zcut policy:** fitur ini gratis, tanpa paywall/watermark, dan tidak memanggil server aplikasi asli.  
> Modul paywall, langganan, kredit, dan iklan **sengaja tidak direkonstruksi**. Semua fitur terbuka.

## Bukti

- Account: `com.lemon.account`, `com.vega.account`, `AccountFacade`,
  `AccountLoginActivity`, `AccountService`, `AccessTokenData`,
  `AccountCache`, `AccountBanDialogFactory`, `AccountDeletedDialog`.
- Monetisasi: `com.lemon.clipmonetize.biz.commerce_page.lv_oversea`
  (2.816 class): `ui/main_page` (421), `ui/base` (235), `ui/sub_page` (199),
  `repo/api` (58), `purchase/precheck` (44), `purchase/failure` (26),
  `purchase/callback` (3), `viewmodel/VipFT*` (free trial), `TradeRecord*`.
- Payment: `biz/payment/{impl,api,data}`, `data/channel`,
  `PaymentFlowEnvironment`, `PayResult`, `TransactionResult`.
- Billing: `com.android.billingclient.api.*` (Google Play Billing).
- GlobalPay / PayPal / Paylution muncul di string/package.
- Ads: `com.applovin`, `com.bytedance.sdk.openadsdk` (Pangle),
  `com.mbridge.msdk`, `com.vungle`, `com.facebook.ads`.
- Asset/paywall: `assets/offline/image_lynx_subscription*` (Lynx),
  `commerce_page_lv_oversea/generated/resources`.
- String SKU: `Weekly`, `7-day free trial`, `capcut_pro`, `credits`,
  `proWeeklyRetainInfo`, `standardWeeklyRetainInfo`, `ai_model_product_id`.

## Paket & entitlement

| Item | Bukti |
|---|---|
| Pro / Standard | `cc_pro_tag`, `standardWeeklyRetainInfo` |
| Weekly / Monthly / Annual | SKU string + `proWeeklyRetainInfo` |
| Free trial 7 hari | locale `7-day free trial`, `VipFT*` |
| Credits | `CreditPageInfoData`, `SUBSCRIBE_FREE_CREDIT_NOT_ENOUGH`, `ai_model_product_id`, `ai writer credits failed` |
| Reward ads | `AITemplateRewardAdComponent`, `loadShowRewardAd` |
| Trade record | `TradeRecordPageVM`, `fetchTradeRecords` |

## Alur pembelian

```
buka paywall (native/Lynx)
  → pilih paket
  → precheck (login, region, existing plan)
  → billing channel:
       Google Play Billing | GlobalPay | PayPal/Paylution
  → purchase
  → verifikasi server (entitlement)
  → update cache + UI
  → restore/acknowledge
```

**Penting:** entitlement harus diverifikasi ke server. Jangan pernah
mempercayai `purchaseState` dari client saja.

## Credit

AI generation/TTS/AI music memakai credit. Alur:

```
cek saldo → tampilkan harga → (reward ad / beli) → debit → task AI
  → gagal? refund
```

Idempotency wajib: satu task hanya boleh mendebit sekali, dan retry tidak
boleh mendebit ulang.

## Ads

| Format | SDK |
|---|---|
| Reward | AppLovin/Pangle/MBridge/Vungle |
| Interstitial | AppLovin/Pangle |
| Splash | Pangle/AppLovin |
| Native/Banner | MBridge/AppLovin |

Reward ad dipakai untuk membuka template AI atau menambah credit. Ada
`AITemplateRewardAdComponent` dan `AITemplateRewardCacheData`.

## Implementasi referensi (clean-room)

```kotlin
data class Entitlement(
    val isPro: Boolean,
    val plan: Plan?,
    val expiresAt: Instant?,
    val credits: Int,
)

interface BillingProvider {
    suspend fun purchase(productId: String): PurchaseResult
    suspend fun restore(): List<PurchaseResult>
}

class SubscriptionManager(
    private val provider: BillingProvider,
    private val backend: EntitlementApi,
    private val cache: EntitlementCache,
) {
    suspend fun buy(productId: String): Entitlement {
        val purchase = provider.purchase(productId)
        val verified = backend.verify(purchase.token, productId)
        cache.put(verified)
        return verified
    }

    suspend fun consumeCredit(cost: Int, taskId: String): Boolean {
        val e = cache.get() ?: return false
        if (e.credits < cost) return false
        return backend.debit(taskId, cost)     // server-authoritative
    }
}
```

Paywall harus selalu menyediakan:
- harga + periode + auto-renew disclosure,
- restore purchase,
- manage/cancel subscription,
- tombol close.

## Catatan paritas

- **P0**: login, paywall statis, satu produk, restore, gate fitur Pro.
- **P1**: free trial, credit, reward ad, riwayat transaksi.
- **P2**: multi-channel payment, regional pricing, family/team plan,
  enterprise.

## Unknown

- SKU persis per region dan harga tidak terbaca.
- Apakah GlobalPay dipakai untuk region tertentu saja.
- Aturan refund dan grace period tidak terbaca.
- Jangan menyertakan token/secret apa pun dari artifact ke dokumen/produk.
