import sys
import os
import time
import webbrowser
import speech_recognition as sr

# Ensure console logs print UTF-8 correctly
sys.stdout.reconfigure(encoding='utf-8')

WAKE_WORDS = ["Pico", "piko", "pick oo", "pick ooh"]

def listen_for_wake():
    r = sr.Recognizer()
    
    # Adjust sensitivity parameters
    r.dynamic_energy_threshold = True
    r.pause_threshold = 0.8
    
    try:
        mic = sr.Microphone()
    except Exception as e:
        print(f"❌ Failed to access microphone: {e}")
        return

    print("🔊 Voice Wake Daemon active. Listening in background for 'Pico'...")

    with mic as source:
        print("⚙️ Adjusting for ambient noise... Please wait.")
        r.adjust_for_ambient_noise(source, duration=1.0)
        print("✅ Ready! Speak 'Pico' to launch the assistant.")

    while True:
        try:
            with mic as source:
                audio = r.listen(source, phrase_time_limit=3.0)
            
            # Use Google Speech Recognition (free, built-in)
            text = r.recognize_google(audio).lower().strip()
            print(f"🎤 Heard: '{text}'")
            
            # Match wake word
            if any(wake in text for wake in WAKE_WORDS):
                print("🚀 Wake word detected! Opening TaskFlow Assistant...")
                # Open browser with a trigger=voice parameter so it starts listening immediately
                webbrowser.open("http://localhost:8000/dashboard?trigger=voice")
                # Sleep briefly to avoid capturing browser startup noises
                time.sleep(4)
        except sr.UnknownValueError:
            # Ignore unintelligible noises
            pass
        except sr.RequestError as e:
            print(f"⚠️ Speech service error: {e}")
            time.sleep(5)
        except KeyboardInterrupt:
            print("\n👋 Exiting background listener daemon.")
            break
        except Exception as e:
            print(f"⚠️ Unexpected error: {e}")
            time.sleep(2)

if __name__ == "__main__":
    listen_for_wake()
