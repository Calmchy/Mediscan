
#define MyAppName "MediScan POS"
#define MyAppVersion "1.1.0"
#define MyAppPublisher "Calmchy"
#define MyAppURL "https://github.com/Calmchy/Mediscan"
#define MyAppExeName "Mediscan POS.exe"

[Setup]
AppId={{A10611E5-5569-4712-995A-1F5F63DA82CB}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}
AppUpdatesURL={#MyAppURL}

DefaultDirName={autopf}\{#MyAppName}
DefaultGroupName={#MyAppName}
UninstallDisplayIcon={app}\{#MyAppExeName}

OutputDir=installer
OutputBaseFilename=MediScan_POS_Setup_v{#MyAppVersion}
SetupIconFile=C:\Users\Calmchy\Desktop\App\logo-128.ico

Compression=lzma2
SolidCompression=yes
WizardStyle=modern

ArchitecturesInstallIn64BitMode=x64

PrivilegesRequired=admin
DisableProgramGroupPage=yes

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; \
    Description: "Create a desktop shortcut"; \
    GroupDescription: "Additional icons:"; \
    Flags: unchecked

[Files]
; Copy the complete application distribution
Source: "C:\Users\Calmchy\Desktop\App\*"; \
    DestDir: "{app}"; \
    Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{autoprograms}\{#MyAppName}"; \
    Filename: "{app}\{#MyAppExeName}"

Name: "{autodesktop}\{#MyAppName}"; \
    Filename: "{app}\{#MyAppExeName}"; \
    Tasks: desktopicon

[Registry]
Root: HKA; \
    Subkey: "Software\Classes\.myp\OpenWithProgids"; \
    ValueType: string; \
    ValueName: "MediScanPOSFile"; \
    ValueData: ""; \
    Flags: uninsdeletevalue

Root: HKA; \
    Subkey: "Software\Classes\MediScanPOSFile"; \
    ValueType: string; \
    ValueName: ""; \
    ValueData: "MediScan POS File"; \
    Flags: uninsdeletekey

Root: HKA; \
    Subkey: "Software\Classes\MediScanPOSFile\DefaultIcon"; \
    ValueType: string; \
    ValueName: ""; \
    ValueData: "{app}\{#MyAppExeName},0"

[Run]
Filename: "{app}\{#MyAppExeName}"; \
    Description: "Launch MediScan POS"; \
    Flags: nowait postinstall skipifsilent
