# Alex rechnet

Android-Homescreen-Widget: zeigt den aktuellen Kurs Euro → Thai-Baht (EZB-Referenzkurs über frankfurter.dev) und rechnet einen eigenen Euro-Betrag um.

- **Widget antippen** → Eingabefenster, Betrag in € eintippen, Baht erscheinen sofort. „Ins Widget" speichert den Betrag.
- **⟳** lädt den Kurs neu. Automatisch alle ca. 3 Stunden und bei jedem Öffnen des Eingabefensters.
- Offline wird der zuletzt geladene Kurs angezeigt.

## Bauen

Jeder Push auf `main` baut über GitHub Actions eine APK und legt sie unter **Releases** ab (`Alex-rechnet.apk`).
