import platform

system = platform.system()

if system == "Darwin":
    print("macOS")
elif system == "Windows":
    print("Windows")
elif system == "Linux":
    print("Linux")
else:
    print("Unknown")
