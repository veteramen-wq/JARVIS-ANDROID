# JARVIS Android 0.1.0

İlk yerel Android prototipi: Kotlin + Jetpack Compose.

## Dahil olanlar
- Türkçe tek seferlik ses tanıma (Android SpeechRecognizer; cihazın tanıma hizmetine bağlı).
- Türkçe sesli yanıt (Android TextToSpeech; Türkçe ses verisi cihazda bulunmalı).
- Saat, tarih, basit dört işlem, yerel not kaydı.
- YouTube, telefon ayarları, harita ve Google araması için Android intent'leri.
- Sohbet geçmişi ve notlar SharedPreferences ile cihazda saklanır.
- Mikrofon izni çalışma anında istenir.

## Henüz olmayanlar
Arka planda sürekli dinleme/wake word, serbest sohbet eden AI modeli, kamera analizi, hava durumu, diğer uygulamaları sınırsız kontrol, otomatik mesaj gönderme ve gelişmiş hafıza yoktur. `EXTRA_PREFER_OFFLINE` çevrimdışı tanımayı garanti etmez.

## Derleme
Android Studio'da bu klasörü açıp Gradle senkronizasyonu yap. Telefon üzerinden APK için GitHub'a yükle, Actions > Build JARVIS APK > Run workflow. Başarılı derleme sonrası `JARVIS-debug-apk` artifact'ini indir.

Gereksinimler: JDK 17, Android SDK 35, Gradle 8.9. Bu kaynak ZIP'inde derlenmiş APK yoktur; henüz CI derlemesi doğrulanmadı.
