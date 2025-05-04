import pyautogui
import time
import math
import sys

def move_mouse_in_circle(radius=2, duration=5, steps=100):
    """
    Moves the mouse cursor in a circle on the screen.

    Args:
        radius (int): The radius of the circle in pixels.
        duration (float): The total time in seconds to complete one circle.
        steps (int): The number of discrete steps to approximate the circle.
    """
    try:
        screen_width, screen_height = pyautogui.size()
        center_x, center_y = screen_width // 2, screen_height // 2
        
        print(f"Screen size: {screen_width}x{screen_height}")
        print(f"Circle center: ({center_x}, {center_y})")
        print(f"Radius: {radius} pixels")
        print(f"Duration: {duration} seconds")
        print(f"Steps: {steps}")
        print("Moving mouse in 3 seconds... Press Ctrl+C to stop.")
        time.sleep(3)

        start_time = time.time()
        
        while True:
            current_time = time.time()
            elapsed_time = current_time - start_time
            
            # Calculate the angle based on the elapsed time within the duration
            # Use modulo to loop the animation
            angle = ((elapsed_time % duration) / duration) * 2 * math.pi
            
            # Calculate the new x and y coordinates
            x = center_x + int(radius * math.cos(angle))
            y = center_y + int(radius * math.sin(angle))

            # Ensure coordinates are within screen bounds
            x = max(0, min(screen_width - 1, x))
            y = max(0, min(screen_height - 1, y))

            # Move the mouse instantly to the new position
            pyautogui.moveTo(x, y)

            # Small delay to prevent overwhelming the system and allow interruption
            # Adjust if needed, but movement is primarily controlled by time now
            time.sleep(0.01) 

    except ImportError:
        print("Error: PyAutoGUI library not found.")
        print("Please install it using: pip install pyautogui")
        sys.exit(1)
    except KeyboardInterrupt:
        print("\nMouse movement stopped by user.")
    except Exception as e:
        print(f"\nAn unexpected error occurred: {e}")

if __name__ == "__main__":
    # --- Configuration ---
    circle_radius = 150  # Pixels
    time_per_circle = 4  # Seconds
    # Steps are less relevant now as movement is time-based, but keep for potential future use
    # number_of_steps = 100 
    # --- End Configuration ---
    
    # Call the function with configured parameters
    # Note: steps parameter is not used in the time-based loop but kept in signature
    move_mouse_in_circle(radius=circle_radius, duration=time_per_circle) 
