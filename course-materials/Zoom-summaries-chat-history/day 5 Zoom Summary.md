## Szybkie podsumowanie

Spotkanie koncentrowało się na omówieniu kwestii technicznych z GitHubem oraz szkoleniu z agentów AI. Sebastian wyjaśnił krytyczny problem z maszyną wirtualną działającą na GitHubie, którego diagnozowanie i rozwiązywanie zajęło 14 godzin, obejmujący problemy z kontrolerem ID, interfejsami sieciowymi i aktualizacjami hiperwizora. Następnie zespół przeszedł do szkolenia na temat agentów AI, gdzie Łukasz Matuszewski zademonstrował różne narzędzia, w tym Playright, Crew AI i Flowwise, do tworzenia zautomatyzowanych testów i rozwoju aplikacji. Uczestnicy podzielili się swoimi postępami w tworzeniu aplikacji z agentami, Sebastian pokazał działającą aplikację generatora memów, a Kris omówił wyzwania związane ze składaniem formularzy i testowaniem. Szkolenie obejmowało najlepsze praktyki pracy z agentami, w tym właściwą dokumentację, procedury testowania i wybór modeli, wraz z dyskusjami na temat różnych modeli sztucznej inteligencji i ich możliwości dla różnych zadań.

## Kolejne kroki

### Łukasz Matuszewski

- Zaktualizuj materiały szkoleniowe, aby zawierały zalecenia dla agentów dotyczące niekodowania nazw modeli i zawsze zakładania, że ich wiedza jest przestarzała.[](https://tasks.zoom.us?meetingId=G%2FoySDG5RNeQWighV3i%2BNw%3D%3D&stepId=ccabcba2-748f-11f1-b073-26f343456850)
- Dodaj do instrukcji agenta punkt dotyczący używania prawidłowych modeli (np. GPT-5.4) zgodnie z instrukcją główną i nie ustawiania domyślnych starszych modeli.[](https://tasks.zoom.us?meetingId=G%2FoySDG5RNeQWighV3i%2BNw%3D%3D&stepId=ccabce63-748f-11f1-955f-26f343456850)
- Wyślij link do repozytorium starszego kodu (klient FTP w Javie) uczestnikom.[](https://tasks.zoom.us?meetingId=G%2FoySDG5RNeQWighV3i%2BNw%3D%3D&stepId=ccabd0fc-748f-11f1-9c62-26f343456850)
- Skontaktuj się z Dominiką (szkolenia) w sprawie niedostępności linku do ankiety.[](https://tasks.zoom.us?meetingId=G%2FoySDG5RNeQWighV3i%2BNw%3D%3D&stepId=ccabd3fa-748f-11f1-9331-26f343456850)
- Zapewnij uczestnikom link polecający do platformy, która oferuje kredyty za tworzenie kont.[](https://tasks.zoom.us?meetingId=G%2FoySDG5RNeQWighV3i%2BNw%3D%3D&stepId=ccabd659-748f-11f1-989b-26f343456850)

### Kris

- Skonfiguruj dostawcę OpenAI w ustawieniach OpenCode, dodając klucz API.[](https://tasks.zoom.us?meetingId=G%2FoySDG5RNeQWighV3i%2BNw%3D%3D&stepId=ccabd89e-748f-11f1-a92d-26f343456850)

### Sebastian

- Zgłoś błąd hiperwizora (problem z kontrolerem ID) odpowiedniemu dostawcy (np. Oracle Virtual Manager).[](https://tasks.zoom.us?meetingId=G%2FoySDG5RNeQWighV3i%2BNw%3D%3D&stepId=ccabc2c1-748f-11f1-8f7b-26f343456850)
- Opracuj i udokumentuj procedurę bezpiecznej aktualizacji maszyny wirtualnej GitHub, w tym wykorzystanie klonów i migawek do testowania aktualizacji przed ich zastosowaniem w głównym środowisku.[](https://tasks.zoom.us?meetingId=G%2FoySDG5RNeQWighV3i%2BNw%3D%3D&stepId=ccabc88f-748f-11f1-a623-26f343456850)

### Współpraca

- Wszyscy Uczestnicy: Wypełnij ankietę szkoleniową, upewniając się o sprawdzeniu opcji niepublikowania wypowiedzi ustnych.[](https://tasks.zoom.us?meetingId=G%2FoySDG5RNeQWighV3i%2BNw%3D%3D&stepId=ccabdad7-748f-11f1-9f99-26f343456850)
- Wszyscy uczestnicy: Sklonuj starsze repozytorium kodu i przeanalizuj jego strukturę, zależności i architekturę w ramach ćwiczenia szkoleniowego.[](https://tasks.zoom.us?meetingId=G%2FoySDG5RNeQWighV3i%2BNw%3D%3D&stepId=ccabdcf5-748f-11f1-9125-26f343456850)

## Podsumowanie

### Problem GitHub i ramy AI

Sebastian omówił krytyczny problem GitHuba, który wystąpił podczas aktualizacji maszyny wirtualnej, gdzie system przestał reagować i jego rozwiązanie wymagało 14 godzin. Przyczyną problemu była kombinacja aktualizacji hiperwizora i problemów z konfiguracją sieci spowodowanych klonowaniem diagnostycznym Dominika, które przypadkowo wyłączyło interfejs sieciowy. Sebastian zaproponował przetestowanie przyszłych aktualizacji na klonach przed wdrożeniem ich w głównym systemie, aby zapobiec podobnym problemom. Zespół omówił również różne frameworki rozwoju sztucznej inteligencji, a Łukasz Matuszewski przedstawił różne narzędzia, w tym Crew AI, Flowise AI i n8n do tworzenia systemów wieloagentowych i automatyzacji przepływów pracy. Marcin podzielił się swoją historią opracowania aplikacji muzycznej OCR, która może odczytywać i podpisywać nuty PDF, a Kris zadał pytania dotyczące modyfikacji istniejących aplikacji oraz różnic technicznych między różnymi frameworkami sztucznej inteligencji.Łukasz Matuszewski zademonstrował testowanie aplikacji przy użyciu zautomatyzowanych agentów i Chrome DevTools, pokazując, w jaki sposób agenci mogą walidować aplikacje internetowe poprzez porównywanie zrzutów ekranu z oczekiwanymi wynikami i wykonywanie ręcznych interakcji w razie potrzeby.

### Dyskusja na temat wdrożenia testów dramaturgicznych

Zespół omówił wdrożenie Playwrighta do testów, a Łukasz Matuszewski wyjaśnił, że oferuje on lepszą funkcjonalność niż poprzednie rozwiązania, takie jak Selenium. Marcin zauważył, że nowsi agenci odchodzą od MCP na rzecz narzędzi takich jak Skill czy Thule. Dyskusja dotyczyła kwestii bezpieczeństwa podczas testowania, z naciskiem na zapewnienie agentom braku dostępu do informacji niepublicznych. Łukasz Matuszewski udostępnił link do repozytorium pokazujący w pełni funkcjonalną aplikację i nakreślił plany wdrożenia funkcji trwałości sesji, w tym szczegółowe przykłady kodu do przyszłej implementacji.

### Planowanie systemów korporacyjnych Wdrażanie procesów

Łukasz Matuszewski i Marcin omówili wdrożenie szczegółowego procesu planowania złożonych aplikacji i systemów korporacyjnych, podkreślając znaczenie dokładnego przygotowania i dokumentacji dla agentów. Zbadano korzyści płynące z rozdziału instrukcji i wytycznych na konkretne pliki i katalogi w celu zapewnienia jasności i łatwości konserwacji, a także rozważono ograniczenia i potencjalne problemy związane z takim podejściem. Rozmowa dotyczyła potrzeby równowagi między uniwersalnymi i wyspecjalizowanymi instrukcjami agentów, a Łukasz Matuszewski sugerował, że umożliwienie głównym agentom dostosowania ich zachowania ad hoc może zapewnić większą elastyczność i kontrolę.

### Dokumentacja projektowa i testowanie modeli

Zespół omówił dokumentację projektową oraz konfiguracje modeli dla agentów. Łukasz Matuszewski wyjaśnił strukturę dokumentacji, w tym PRD, ADR i wytyczne projektowe, z oddzielną dokumentacją dla agentów i ogólnymi instrukcjami. Grupa testowała różne konfiguracje modeli, koncentrując się w szczególności na modelu openai/gpt-5.4-mini, który pomyślnie przetworzył zgłoszenie i przeniósł się do widoku czatu z oceną sztucznej inteligencji. Kris zgłosił problemy z integracją Playlight, w których system był niestabilny i wymagał ręcznej interwencji, chociaż ostatecznie działał poprawnie. Sebastian zademonstrował działającą aplikację generatora memów, którą opracował lokalnie przy użyciu Open Routera i MariaDB, która zawierała funkcjonalność logowania i wylogowywania.

### Analiza starszych aplikacji FTP Java

Łukasz Matuszewski omówił kilka trendów w tworzeniu aplikacji, m.in. wykorzystanie agentów do wdrażania, pojawienie się platform ułatwiających wdrażanie oraz trend wykorzystania kart graficznych o niskiej mocy do szkolenia modeli AI. Zespół przeanalizował starą aplikację kliencką FTP Java, która wymagała analizy i dokumentacji zależności i architektury. Sebastian wyraził obawy dotyczące pobierania zależności lokalnie, a Łukasz Matuszewski przydzielił zadania analizy struktury kodu, zależności i wzorców projektowych przed próbą uruchomienia aplikacji.

### Refaktoryzacja starszych aplikacji FTP Java

Zespół omówił strategie testowania i refaktoryzacji starszej aplikacji Java FTP, która ma 14 lat i obecnie nie działa. Łukasz Matuszewski podkreślił znaczenie tworzenia testów jednostkowych, które koncentrują się na funkcjonalności, a nie szczegółach implementacji, umożliwiając przyszły refaktoryz kodu przy zachowaniu istniejącej funkcjonalności. Zespół wykorzystał do analizy kodu specjalistyczne modele sztucznej inteligencji, takie jak Gemma-4, a Marcin podzielił się informacjami na temat różnych wersji modeli i ich możliwości. Łukasz Matuszewski dostarczył szczegółowe instrukcje dotyczące analizy struktury aplikacji, w tym zbadania co najmniej 10 kluczowych plików w celu zrozumienia standardów kodu, wersji Java, wzorców projektowych i ogólnej architektury przed wygenerowaniem podsumowania.

### Implementacja struktury plików AGENTS.md

Łukasz Matuszewski i Adam omówili wdrożenie struktury plików AGENTS.md dla projektu, przy czym Łukasz Matuszewski zaproponował plik główny na poziomie projektu zawierający ogólne opisy, podczas gdy szczegółowe instrukcje znajdowałyby się w konkretnych folderach. Adam potwierdził, że implementacja zadziałała dla niego. Marcin wspomniał o ostatnich ulepszeniach modeli lokalnych, zauważając, że model 35B został wydany do aplikacji agentowych z lepszymi wynikami. Rozmowa zakończyła się tym, że Łukasz Matuszewski ogłosił przerwę i planuje wznowienie spotkania o godzinie 2:00 lub 10:00.

### Aktualizacje ankiety i testowania Łukasz Matuszewski

Łukasz Matuszewski poprosił uczestników o wypełnienie ankiet, ale niektórzy uczestnicy doświadczyli trudności technicznych z dostępem do linków do ankiet. Łukasz Matuszewski planował współpracować z Moniką w celu rozwiązania problemów i zapewnienia dystrybucji prawidłowych linków do ankiet. Następnie dyskusja przeniosła się do wdrożenia narzędzi Mermaid i umiejętności testowania Java jUnit 5 w celu analizy przepływów danych projektowych oraz pisania testów jednostkowych dla kluczowych komponentów. Marcin wspomniał o testowaniu modelu Gemma 12B z 12 miliardami parametrów. Łukasz Matuszewski zauważył skuteczność modelu GLM 5.2 w generowaniu naturalnie brzmiących opisów postaci do NFT dla córki i do treści marketingowych oraz UI.

### Konfiguracja narzędzi do przeglądania kodu AI

Spotkanie koncentrowało się na omówieniu konfiguracji i wykorzystania narzędzi do recenzowania kodu AI, w szczególności OpenCode i CloudCode, do analizy i recenzowania starszych aplikacji Java. Łukasz Matuszewski dostarczył wskazówek dotyczących konfiguracji tych narzędzi, w tym konfigurowania dostawców i obsługi operacji w trybie bezgłowym, a także rozwiązała konkretne problemy techniczne poruszone przez uczestników takich jak Kris dotyczące ograniczeń zasobów i dostępu do modeli. Dyskusja dotyczyła również alternatywnych narzędzi, takich jak Qodo do przeglądania kodu oraz różnych platform do tworzenia i testowania aplikacji, przy czym Łukasz Matuszewski udostępnia linki do dokumentacji i przykłady wspierające dalszą implementację.
