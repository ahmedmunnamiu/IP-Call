# IP CALL — SIP Dialer (Liquid Glass UI)

যেকোনো SIP অ্যাকাউন্ট দিয়ে লগইন করে কল করা ও রিসিভ করার Android অ্যাপ।
Developer: **AHMED MUNNA**

## ফিচার
- যেকোনো SIP অ্যাকাউন্টে লগইন (Username, Password, Server; Advanced: Auth user, Proxy, Port, UDP/TCP/TLS)
- কল করা, কল রিসিভ/রিজেক্ট, Mute, Speaker, Hold, কলের ভিতরে DTMF কিপ্যাড
- রেফারেন্স ছবির মতো গোলাকার-স্কয়ার গ্লাস কিপ্যাড (Make a call / CALL / backspace)
- লিকুইড গ্লাস UI সব জায়গায় (ব্লার + হাইলাইট + গ্লসি বর্ডার; ব্লার Android 12+ এ)
- Settings → Appearance থেকে ব্যাকগ্রাউন্ড বদলানো: ৮টি প্রিসেট অথবা গ্যালারির ছবি (Dim/Blur সহ), Glass blur/brightness, অ্যানিমেটেড ব্যাকগ্রাউন্ড অন/অফ
- সব ট্রানজিশনে স্মুথ অ্যানিমেশন (ট্যাব, লগইন→হোম, কল স্ক্রিন, About, বাটন প্রেস)
- Recent calls, ব্যাকগ্রাউন্ডে কল রিসিভের জন্য ফোরগ্রাউন্ড সার্ভিস
- About স্ক্রিনে "AHMED MUNNA"

## GitHub থেকে APK বানানোর নিয়ম
1. GitHub এ নতুন Repository বানান।
2. এই ZIP এক্সট্র্যাক্ট করে ভিতরের **সব ফাইল ও ফোল্ডার** (লুকানো `.github` ফোল্ডারসহ) আপলোড করুন।
   - যদি `.github` ফোল্ডার আপলোড না হয়: Repository তে **Add file → Create new file** চেপে নাম দিন
     `.github/workflows/build-apk.yml` এবং এই ZIP এর ভিতরের একই নামের ফাইলের লেখা পেস্ট করুন।
3. **Actions** ট্যাবে `Build IP CALL APK` চলবে (৫–১০ মিনিট)। না চললে Run workflow চাপুন।
4. শেষ হলে ওই run খুলে নিচে **Artifacts → IP-CALL-apk** ডাউনলোড করুন, ZIP খুলে APK ইনস্টল করুন।

Android Studio তে খুললেও সরাসরি বিল্ড হবে (JDK 17)।

## জানা দরকার
- **ইনকামিং কল ব্যাকগ্রাউন্ডে**: পুশ সার্ভার ছাড়া অ্যাপ চালু থাকা লাগে। তাই Settings → Calls এ "Receive calls in background" অন রাখুন এবং Battery optimization থেকে IP CALL কে Unrestricted করুন।
- **লক স্ক্রিনে ফুল-স্ক্রিন কল**: Android 14+ এ ফোন সেটিংস থেকে "Full screen notifications" এর অনুমতি দিতে হতে পারে।
- **SIP ইঞ্জিন Linphone SDK (GPLv3)**: অ্যাপ অন্যদের কাছে বিতরণ বা Play Store এ দিলে GPLv3 মানতে হবে (সোর্স কোড দিতে হবে) অথবা Belledonne এর কমার্শিয়াল লাইসেন্স নিতে হবে। নিজের ব্যবহারের জন্য সমস্যা নেই।
- APK টি debug কী দিয়ে সাইন করা, নিজের ফোনে ইনস্টলের জন্য ঠিক আছে।
