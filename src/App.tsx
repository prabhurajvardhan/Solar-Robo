import React, { useState } from 'react';
import {
  Layers,
  Cpu,
  FileCode,
  ShieldAlert,
  FolderTree,
  Terminal,
  Activity,
  Sun,
  Battery,
  Wind,
  Video,
  MessageSquare,
  Sliders,
  Bell,
  CheckCircle2,
  Copy,
  Check,
  ChevronRight,
  ExternalLink,
  BookOpen
} from 'lucide-react';

interface ModuleDef {
  id: string;
  name: string;
  title: string;
  task: string;
  icon: any;
  inputs: string;
  outputs: string;
  forbidden: string[];
  files: { name: string; type: string; desc: string; code: string }[];
}

const MODULES: ModuleDef[] = [
  {
    id: 'onboarding',
    name: 'Onboarding',
    title: 'Device Onboarding (:feature:onboarding)',
    task: 'Discover, connect, and configure a Solar Robo unit via Bluetooth Low Energy (BLE) or Wi-Fi without operating the panel.',
    icon: Cpu,
    inputs: 'DeviceCandidate[] (BLE discovery), DeviceConfig (user setup)',
    outputs: 'OnboardingResult, DeviceAdded event',
    forbidden: [
      'No panel movement or angle actuation',
      'No energy optimization logic',
      'No AI model invocations',
      'No safety gate alterations'
    ],
    files: [
      {
        name: 'OnboardingModule.kt',
        type: 'DI Module',
        desc: 'Hilt module providing OnboardingRepository singleton binding',
        code: `@Module\n@InstallIn(SingletonComponent::class)\nabstract class OnboardingModule {\n    @Binds\n    @Singleton\n    abstract fun bindOnboardingRepository(impl: OnboardingRepositoryImpl): OnboardingRepository\n}`
      },
      {
        name: 'domain/OnboardingRepository.kt',
        type: 'Domain Interface',
        desc: 'Repository contract defining device discovery & pairing methods',
        code: `data class DeviceCandidate(val id: String, val name: String, val rssi: Int)\ndata class DeviceConfig(val deviceId: String, val displayName: String, val wifiSsid: String, val wifiPass: String)\nsealed interface OnboardingResult { data class Success(val id: String): OnboardingResult; data class Failure(val msg: String): OnboardingResult }\n\ninterface OnboardingRepository {\n    fun scanDevices(): Flow<List<DeviceCandidate>>\n    suspend fun connectDevice(deviceId: String): Result<Unit>\n    suspend fun saveConfig(config: DeviceConfig): Result<Unit>\n    suspend fun complete(config: DeviceConfig): OnboardingResult\n}`
      },
      {
        name: 'presentation/OnboardingViewModel.kt',
        type: 'Presentation ViewModel',
        desc: 'MVVM StateFlow manager handling discovery and pairing flow',
        code: `@HiltViewModel\nclass OnboardingViewModel @Inject constructor(\n    private val repository: OnboardingRepository\n) : ViewModel() {\n    private val _uiState = MutableStateFlow(OnboardingUiState())\n    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()\n\n    fun startScanning() = viewModelScope.launch {\n        _uiState.update { it.copy(isScanning = true) }\n        repository.scanDevices().collect { list ->\n            _uiState.update { it.copy(isScanning = false, devices = list) }\n        }\n    }\n}`
      },
      {
        name: 'presentation/OnboardingScreen.kt',
        type: 'Compose UI Screen',
        desc: 'Declarative screen with device list, pairing status, and setup fields',
        code: `@Composable\nfun OnboardingScreen(viewModel: OnboardingViewModel) {\n    val state by viewModel.uiState.collectAsState()\n    Scaffold(topBar = { TopAppBar(title = { Text("Connect Solar Robo") }) }) {\n        // Discovery & Configuration list\n    }\n}`
      }
    ]
  },
  {
    id: 'home',
    name: 'Home',
    title: 'Home Command Center (:feature:home)',
    task: 'Read-only dashboard presenting real-time status assembled from standardized contracts: tracker angle, generation watts, battery %, and active safety alerts.',
    icon: Activity,
    inputs: 'RoboSnapshot, EnergySnapshot, SafetyEvent[]',
    outputs: 'HomeUiState, NavigationIntent',
    forbidden: [
      'No physical actuation or commands',
      'No AI or LLM reasoning calls',
      'No safety policy decisions',
      'No source data ownership'
    ],
    files: [
      {
        name: 'domain/HomeRepository.kt',
        type: 'Domain Interface',
        desc: 'Aggregates streams from RoboDevice, Energy, and Safety contracts',
        code: `interface HomeRepository {\n    fun getRoboSnapshot(): Flow<RoboSnapshot>\n    fun getEnergySnapshot(): Flow<EnergySnapshot>\n    fun getActiveSafetyEvents(): Flow<List<SafetyEvent>>\n}`
      },
      {
        name: 'presentation/HomeViewModel.kt',
        type: 'Presentation ViewModel',
        desc: 'Combines reactive snapshot flows into an immutable HomeUiState',
        code: `@HiltViewModel\nclass HomeViewModel @Inject constructor(\n    private val repository: HomeRepository\n) : ViewModel() {\n    val uiState: StateFlow<HomeUiState> = combine(\n        repository.getRoboSnapshot(),\n        repository.getEnergySnapshot(),\n        repository.getActiveSafetyEvents()\n    ) { robo, energy, alerts ->\n        HomeUiState(isLoading = false, robo = robo, energy = energy, alerts = alerts)\n    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState(isLoading = true))\n}`
      },
      {
        name: 'presentation/HomeScreen.kt',
        type: 'Compose UI Screen',
        desc: 'Renders RoboStatusCard, EnergySummaryCard, and AlertSummaryCard',
        code: `@Composable\nfun HomeScreen(viewModel: HomeViewModel, onNavigate: (String) -> Unit) {\n    val state by viewModel.uiState.collectAsState()\n    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {\n        state.robo?.let { item { RoboStatusCard(robo = it) } }\n        state.energy?.let { item { EnergySummaryCard(energy = it) } }\n    }\n}`
      }
    ]
  },
  {
    id: 'energy',
    name: 'Energy',
    title: 'Energy Center (:feature:energy)',
    task: 'Display real-time power generation, household consumption, battery reserve, and historical yield. Never control physical hardware.',
    icon: Sun,
    inputs: 'EnergySnapshot (real-time telemetry), EnergyHistoryPoint[] (stored history)',
    outputs: 'EnergyUiState, EnergyRangeRequest',
    forbidden: [
      'No panel movement or actuator actuation',
      'No battery-limit alterations',
      'No grid export relay actuation',
      'No weather forecasting',
      'No fabricated data points'
    ],
    files: [
      {
        name: 'domain/CalculateEnergyMetrics.kt',
        type: 'Domain Use Case',
        desc: 'Deterministic calculation of total Watt-hours generated',
        code: `class CalculateEnergyMetrics @Inject constructor() {\n    operator fun invoke(p: List<EnergyHistoryPoint>): Float =\n        p.sumOf { it.generatedWh.toDouble() }.toFloat()\n}`
      },
      {
        name: 'domain/EnergyRepository.kt',
        type: 'Domain Interface',
        desc: 'Observes current snapshot and queries historical points',
        code: `enum class HistoryRange { TODAY, WEEK, MONTH }\ninterface EnergyRepository {\n    fun observeEnergy(): Flow<EnergySnapshot>\n    suspend fun getHistory(range: HistoryRange): List<EnergyHistoryPoint>\n}`
      },
      {
        name: 'presentation/EnergyViewModel.kt',
        type: 'Presentation ViewModel',
        desc: 'Manages selected history range and computes aggregated yield',
        code: `@HiltViewModel\nclass EnergyViewModel @Inject constructor(\n    private val repository: EnergyRepository,\n    private val calculateMetrics: CalculateEnergyMetrics\n) : ViewModel() {\n    // Observes real-time snapshot and triggers range queries\n}`
      }
    ]
  },
  {
    id: 'control',
    name: 'Control',
    title: 'Robo Control (:feature:control)',
    task: 'Manual/automatic panel angle adjustment and mode controls. Movement commands MUST pass through the Safety Boundary.',
    icon: Sliders,
    inputs: 'RoboSnapshot, User angle requests',
    outputs: 'RoboCommand, CommandResult',
    forbidden: [
      'No direct GPIO or motor driver pin access',
      'No raw ESP32 serial protocol in UI',
      'No AI reasoning deciding movement',
      'Never bypass the safety gate'
    ],
    files: [
      {
        name: 'domain/ControlRepository.kt',
        type: 'Domain Interface',
        desc: 'Coordinates commands through the safety-gated execution pipeline',
        code: `interface ControlRepository {\n    fun observeTrackerState(): Flow<RoboSnapshot>\n    suspend fun executeCommand(command: RoboCommand): CommandResult\n}`
      },
      {
        name: 'presentation/ControlViewModel.kt',
        type: 'Presentation ViewModel',
        desc: 'Dispatches validated MoveToAngle, StopMotion, and SafePosition commands',
        code: `fun setAngle(angle: Float) {\n    val validated = angle.coerceIn(-90f, 90f)\n    viewModelScope.launch {\n        repository.executeCommand(RoboCommand.MoveToAngle(validated))\n    }\n}\nfun emergencyStop() = viewModelScope.launch { repository.executeCommand(RoboCommand.StopMotion) }\nfun moveToSafePosition() = viewModelScope.launch { repository.executeCommand(RoboCommand.SafePosition) }`
      }
    ]
  },
  {
    id: 'safety',
    name: 'Safety',
    title: 'Safety & Emergency Gate (:feature:safety)',
    task: 'Deterministic physical-command gate, emergency stop enforcement, safe stow positioning, and incident ledger.',
    icon: ShieldAlert,
    inputs: 'RoboCommand, EnvironmentSnapshot, DeviceHealth',
    outputs: 'SafetyDecision (ALLOW, BLOCK, MODIFY), SafetyEvent, Safe command',
    forbidden: [
      'No LLM/AI safety decisions (must remain strictly deterministic Kotlin code)',
      'No silent threshold changes at runtime',
      'No duplicate ownership of telemetry'
    ],
    files: [
      {
        name: 'domain/SafetyPolicy.kt',
        type: 'Deterministic Safety Gate',
        desc: 'Evaluates commands against wind speed, battery reserves, and mechanical limits',
        code: `class SafetyPolicy @Inject constructor() {\n    fun evaluate(command: RoboCommand, context: SafetyContext): SafetyDecision {\n        if (context.environment.windSpeedMps > 15.0f) {\n            return SafetyDecision.Modify(RoboCommand.SafePosition, "High wind: force safe stow.")\n        }\n        if (context.health.motorStatus == HealthStatus.FAULT) {\n            return SafetyDecision.Block("Motor fault active.")\n        }\n        if (command is RoboCommand.MoveToAngle && command.angleDeg !in -85f..85f) {\n            return SafetyDecision.Modify(RoboCommand.MoveToAngle(command.angleDeg.coerceIn(-85f, 85f)), "Clamped to mechanical stop.")\n        }\n        return SafetyDecision.Allow\n    }\n}`
      },
      {
        name: 'domain/SafetyRepository.kt',
        type: 'Domain Interface',
        desc: 'Manages emergency stops, incidents, and command gating',
        code: `interface SafetyRepository {\n    fun observeSafetyEvents(): Flow<List<SafetyEvent>>\n    suspend fun evaluateAndExecute(command: RoboCommand): SafetyDecision\n    suspend fun triggerEmergencyStop(reason: String): CommandResult\n    suspend fun requestSafeStow(reason: String): CommandResult\n}`
      }
    ]
  },
  {
    id: 'talk',
    name: 'Talk',
    title: 'Robo Talk Assistant (:feature:talk)',
    task: 'Conversational assistant for querying system status and efficiency. AI inference remains strictly isolated behind AiEngine.',
    icon: MessageSquare,
    inputs: 'User text/transcript, System context map, AiEngine',
    outputs: 'ConversationMessage, Optional AI intent',
    forbidden: [
      'No direct hardware actuation from LLM responses',
      'No safety overrides',
      'No model SDK calls directly in UI composables',
      'No unlimited raw history dumped into model context'
    ],
    files: [
      {
        name: 'domain/TalkRepository.kt',
        type: 'Domain Interface',
        desc: 'Chat history and bounded AI prompt generation',
        code: `data class ChatMessage(val id: String, val sender: MessageSender, val text: String, val timestamp: Long)\ninterface TalkRepository {\n    fun getMessageHistory(): Flow<List<ChatMessage>>\n    suspend fun sendMessage(userText: String): ChatMessage\n}`
      },
      {
        name: 'presentation/TalkViewModel.kt',
        type: 'Presentation ViewModel',
        desc: 'Handles chat submission, thinking state, and AI responses',
        code: `@HiltViewModel\nclass TalkViewModel @Inject constructor(\n    private val repository: TalkRepository\n) : ViewModel() {\n    fun send(text: String) = viewModelScope.launch {\n        _uiState.update { it.copy(isThinking = true) }\n        repository.sendMessage(text)\n        _uiState.update { it.copy(isThinking = false) }\n    }\n}`
      }
    ]
  },
  {
    id: 'camera',
    name: 'Camera',
    title: 'Live Camera (:feature:camera)',
    task: 'CameraX preview, connection, and still snapshots for panel optical alignment and dust inspection. Vision reasoning is separate.',
    icon: Video,
    inputs: 'CameraState / Video frames, User capture action',
    outputs: 'CameraUiState, CameraFrame',
    forbidden: [
      'No facial recognition or biometrics',
      'No Vision-Language Model reasoning in camera module',
      'No motor actuation decisions derived from image pixels',
      'No continuous video persistence to cloud without user request'
    ],
    files: [
      {
        name: 'domain/CameraRepository.kt',
        type: 'Domain Interface',
        desc: 'Streams CameraX preview and captures high-res frames',
        code: `interface CameraRepository {\n    fun observeConnectionState(): Flow<CameraConnectionState>\n    suspend fun startStream(): Result<Unit>\n    suspend fun stopStream()\n    suspend fun captureSnapshot(): Result<CameraFrame>\n}`
      }
    ]
  },
  {
    id: 'environment',
    name: 'Environment',
    title: 'Local Environment (:feature:environment)',
    task: 'Presents local sensor measurements: solar irradiance (lux), panel temperature, wind speed, humidity, and rain detection.',
    icon: Wind,
    inputs: 'EnvironmentSnapshot from weather sensor cluster',
    outputs: 'EnvironmentUiState, ConditionEvent',
    forbidden: [
      'No weather forecast engine',
      'No external 3rd party weather APIs unless assigned',
      'No panel movement',
      'No safety thresholds defined in UI'
    ],
    files: [
      {
        name: 'domain/EnvironmentRepository.kt',
        type: 'Domain Interface',
        desc: 'Streams real-time weather and irradiance telemetry',
        code: `interface EnvironmentRepository {\n    fun observeEnvironment(): Flow<EnvironmentSnapshot>\n}`
      }
    ]
  },
  {
    id: 'simulator',
    name: 'Simulator',
    title: 'Developer Simulator (:feature:simulator)',
    task: 'Deterministic fake world generating authentic production contracts for rapid UI iteration and offline edge-case testing.',
    icon: Terminal,
    inputs: 'ScenarioConfig (preset scenarios), tick(dt)',
    outputs: 'RoboSnapshot, EnergySnapshot, EnvironmentSnapshot, SafetyEvent',
    forbidden: [
      'Never connect to real ESP32 hardware',
      'Never replace production adapter in release builds',
      'No hidden non-deterministic randomness in tests',
      'No production threshold changes'
    ],
    files: [
      {
        name: 'domain/Scenario.kt',
        type: 'Simulation Presets',
        desc: 'Enumerates 7 standard deterministic testing scenarios',
        code: `enum class Scenario {\n    SUNNY_NORMAL, LOW_LIGHT, HIGH_WIND, RAIN, MOTOR_JAM, BATTERY_LOW, DEVICE_OFFLINE\n}`
      },
      {
        name: 'domain/SimulatorRepository.kt',
        type: 'Domain Interface',
        desc: 'Controls simulated physical variables and injects faults',
        code: `interface SimulatorRepository {\n    fun getRoboSnapshot(): Flow<RoboSnapshot>\n    fun getEnergySnapshot(): Flow<EnergySnapshot>\n    fun getEnvironmentSnapshot(): Flow<EnvironmentSnapshot>\n    suspend fun applyScenario(scenario: Scenario)\n    suspend fun injectFault(code: String, message: String): SafetyEvent\n}`
      }
    ]
  }
];

export default function App() {
  const [activeTab, setActiveTab] = useState<'overview' | 'techstack' | 'contracts' | 'modules' | 'agents' | 'simulator'>('overview');
  const [selectedModuleId, setSelectedModuleId] = useState<string>('home');
  const [copiedCode, setCopiedCode] = useState<string | null>(null);

  // Simulator live demo state
  const [simScenario, setSimScenario] = useState<string>('SUNNY_NORMAL');
  const [simAngle, setSimAngle] = useState<number>(38);
  const [simWatts, setSimWatts] = useState<number>(320);
  const [simBattery, setSimBattery] = useState<number>(94);
  const [simWind, setSimWind] = useState<number>(4.2);
  const [simRain, setSimRain] = useState<boolean>(false);
  const [simSafetyState, setSimSafetyState] = useState<string>('NORMAL (Nominal tracking)');

  // Onboarding live preview state
  const [onbStep, setOnbStep] = useState<'DISCOVERY' | 'CONFIGURATION' | 'COMPLETED'>('DISCOVERY');
  const [onbScanning, setOnbScanning] = useState(false);
  const [onbDevices, setOnbDevices] = useState<{ id: string; name: string; rssi: number }[]>([]);
  const [onbSelectedDevice, setOnbSelectedDevice] = useState<{ id: string; name: string; rssi: number } | null>(null);
  const [onbConnecting, setOnbConnecting] = useState(false);
  const [onbName, setOnbName] = useState('Solar Robo Rooftop');
  const [onbSsid, setOnbSsid] = useState('Home-WiFi');
  const [onbPass, setOnbPass] = useState('solarpass123');

  const handleScenarioChange = (sc: string) => {
    setSimScenario(sc);
    if (sc === 'SUNNY_NORMAL') {
      setSimWatts(335);
      setSimWind(3.5);
      setSimRain(false);
      setSimBattery(95);
      setSimSafetyState('NORMAL (Nominal tracking)');
    } else if (sc === 'HIGH_WIND') {
      setSimWind(18.5);
      setSimRain(false);
      setSimSafetyState('MODIFY -> SafePosition (0° Stow due to 18.5 m/s wind)');
      setSimAngle(0);
    } else if (sc === 'RAIN') {
      setSimWatts(60);
      setSimRain(true);
      setSimWind(8.0);
      setSimSafetyState('PROTECTING (Rain sensor triggered moisture stow)');
    } else if (sc === 'MOTOR_JAM') {
      setSimSafetyState('BLOCK (Motor stall fault detected on azimuth axis)');
    } else if (sc === 'BATTERY_LOW') {
      setSimBattery(8);
      setSimSafetyState('BLOCK (Low battery conservation mode active)');
    }
  };

  const copyToClipboard = (text: string, id: string) => {
    navigator.clipboard.writeText(text);
    setCopiedCode(id);
    setTimeout(() => setCopiedCode(null), 2000);
  };

  const currentModule = MODULES.find(m => m.id === selectedModuleId) || MODULES[0];

  return (
    <div className="flex h-screen bg-slate-950 text-slate-100 font-sans overflow-hidden">
      {/* Sidebar Navigation */}
      <aside className="w-72 bg-slate-900 border-r border-slate-800 flex flex-col justify-between">
        <div>
          {/* Header */}
          <div className="p-5 border-b border-slate-800 flex items-center space-x-3">
            <div className="p-2 bg-amber-500/10 text-amber-400 rounded-lg border border-amber-500/20">
              <Sun className="w-6 h-6 animate-pulse" />
            </div>
            <div>
              <h1 className="font-bold text-base tracking-wide text-white">SOLAR ROBO</h1>
              <span className="text-xs text-slate-400 font-mono">Android Specification</span>
            </div>
          </div>

          {/* Navigation Links */}
          <nav className="p-3 space-y-1">
            <button
              onClick={() => setActiveTab('overview')}
              className={`w-full flex items-center space-x-3 px-3 py-2.5 rounded-lg text-sm font-medium transition ${
                activeTab === 'overview'
                  ? 'bg-amber-500 text-slate-950 font-semibold'
                  : 'text-slate-300 hover:bg-slate-800 hover:text-white'
              }`}
            >
              <Layers className="w-4 h-4" />
              <span>System Architecture</span>
            </button>

            <button
              onClick={() => setActiveTab('techstack')}
              className={`w-full flex items-center space-x-3 px-3 py-2.5 rounded-lg text-sm font-medium transition ${
                activeTab === 'techstack'
                  ? 'bg-amber-500 text-slate-950 font-semibold'
                  : 'text-slate-300 hover:bg-slate-800 hover:text-white'
              }`}
            >
              <Cpu className="w-4 h-4" />
              <span>Locked Tech Stack</span>
            </button>

            <button
              onClick={() => setActiveTab('contracts')}
              className={`w-full flex items-center space-x-3 px-3 py-2.5 rounded-lg text-sm font-medium transition ${
                activeTab === 'contracts'
                  ? 'bg-amber-500 text-slate-950 font-semibold'
                  : 'text-slate-300 hover:bg-slate-800 hover:text-white'
              }`}
            >
              <FileCode className="w-4 h-4" />
              <span>Canonical Contracts</span>
            </button>

            <button
              onClick={() => setActiveTab('modules')}
              className={`w-full flex items-center space-x-3 px-3 py-2.5 rounded-lg text-sm font-medium transition ${
                activeTab === 'modules'
                  ? 'bg-amber-500 text-slate-950 font-semibold'
                  : 'text-slate-300 hover:bg-slate-800 hover:text-white'
              }`}
            >
              <FolderTree className="w-4 h-4" />
              <span>Module Specifications</span>
            </button>

            <button
              onClick={() => setActiveTab('agents')}
              className={`w-full flex items-center space-x-3 px-3 py-2.5 rounded-lg text-sm font-medium transition ${
                activeTab === 'agents'
                  ? 'bg-amber-500 text-slate-950 font-semibold'
                  : 'text-slate-300 hover:bg-slate-800 hover:text-white'
              }`}
            >
              <Terminal className="w-4 h-4" />
              <span>AGENTS.md & Rules</span>
            </button>

            <button
              onClick={() => setActiveTab('simulator')}
              className={`w-full flex items-center space-x-3 px-3 py-2.5 rounded-lg text-sm font-medium transition ${
                activeTab === 'simulator'
                  ? 'bg-amber-500 text-slate-950 font-semibold'
                  : 'text-slate-300 hover:bg-slate-800 hover:text-white'
              }`}
            >
              <Activity className="w-4 h-4" />
              <span>Interactive Simulator</span>
            </button>
          </nav>
        </div>

        {/* Footer info */}
        <div className="p-4 border-t border-slate-800 bg-slate-900/50">
          <div className="text-xs text-slate-400 space-y-1">
            <p className="flex items-center justify-between">
              <span>Android Target:</span>
              <span className="text-emerald-400 font-mono">API 26-35</span>
            </p>
            <p className="flex items-center justify-between">
              <span>UI Framework:</span>
              <span className="text-amber-400 font-mono">Compose M3</span>
            </p>
            <p className="flex items-center justify-between">
              <span>Dev Server:</span>
              <span className="text-emerald-400 font-mono">Port 3000 Active</span>
            </p>
          </div>
        </div>
      </aside>

      {/* Main Content Area */}
      <main className="flex-1 overflow-y-auto bg-slate-950 p-8">
        {/* TAB 1: OVERVIEW & SYSTEM ARCHITECTURE */}
        {activeTab === 'overview' && (
          <div className="max-w-5xl mx-auto space-y-8">
            <div className="border-b border-slate-800 pb-5">
              <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-amber-500/10 text-amber-400 text-xs font-medium border border-amber-500/20 mb-3">
                <span>Architecture Blueprint</span>
                <span>•</span>
                <span>Native Android</span>
              </div>
              <h2 className="text-3xl font-extrabold text-white">System Architecture & Clean Design</h2>
              <p className="text-slate-400 mt-2 text-base">
                Core principle: <em>Create the puzzle piece completely → verify it → place it in the correct position.</em>
              </p>
            </div>

            {/* Architecture Invariants Grid */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div className="p-5 rounded-xl bg-slate-900 border border-slate-800 space-y-2">
                <div className="flex items-center space-x-2 text-amber-400 font-semibold">
                  <ShieldAlert className="w-5 h-5" />
                  <h3>Deterministic Safety</h3>
                </div>
                <p className="text-sm text-slate-400 leading-relaxed">
                  Large Language Models (AI) can advise or explain, but physical commands <strong>NEVER bypass</strong> the deterministic Safety Gate.
                </p>
              </div>

              <div className="p-5 rounded-xl bg-slate-900 border border-slate-800 space-y-2">
                <div className="flex items-center space-x-2 text-sky-400 font-semibold">
                  <FolderTree className="w-5 h-5" />
                  <h3>Feature Isolation</h3>
                </div>
                <p className="text-sm text-slate-400 leading-relaxed">
                  Feature modules never import other feature modules. Cross-boundary data uses canonical contracts in <code className="text-xs text-sky-300">:core:contracts</code>.
                </p>
              </div>

              <div className="p-5 rounded-xl bg-slate-900 border border-slate-800 space-y-2">
                <div className="flex items-center space-x-2 text-emerald-400 font-semibold">
                  <Terminal className="w-5 h-5" />
                  <h3>Mock-First & Preview</h3>
                </div>
                <p className="text-sm text-slate-400 leading-relaxed">
                  Every feature implements a <code className="text-xs text-emerald-300">FakeRepository</code> allowing full Compose <code className="text-xs text-emerald-300">@Preview</code> and testing without hardware.
                </p>
              </div>
            </div>

            {/* 3-Tier Layer Diagram */}
            <div className="bg-slate-900 p-6 rounded-2xl border border-slate-800 space-y-4">
              <h3 className="text-lg font-bold text-white flex items-center space-x-2">
                <Layers className="w-5 h-5 text-amber-400" />
                <span>Multi-Module Layer Structure</span>
              </h3>
              
              <div className="space-y-3 font-mono text-xs">
                <div className="p-4 bg-slate-950 rounded-xl border border-slate-800 text-center">
                  <div className="font-bold text-amber-400 text-sm mb-1">:app MODULE</div>
                  <div className="text-slate-400">SolarRoboApplication (Hilt Root) • MainActivity • AppNavHost (Navigation Compose)</div>
                </div>

                <div className="flex justify-center text-slate-600 font-bold">↓ composes & wires navigation ↓</div>

                <div className="p-4 bg-slate-950 rounded-xl border border-sky-900/50">
                  <div className="font-bold text-sky-400 text-sm mb-2 text-center">:feature MODULES (Strictly Isolated)</div>
                  <div className="grid grid-cols-2 md:grid-cols-4 gap-2 text-center text-slate-300">
                    <span className="p-1.5 bg-slate-900 rounded border border-slate-800">:feature:onboarding</span>
                    <span className="p-1.5 bg-slate-900 rounded border border-slate-800">:feature:home</span>
                    <span className="p-1.5 bg-slate-900 rounded border border-slate-800">:feature:energy</span>
                    <span className="p-1.5 bg-slate-900 rounded border border-slate-800">:feature:control</span>
                    <span className="p-1.5 bg-slate-900 rounded border border-slate-800">:feature:talk</span>
                    <span className="p-1.5 bg-slate-900 rounded border border-slate-800">:feature:safety</span>
                    <span className="p-1.5 bg-slate-900 rounded border border-slate-800">:feature:camera</span>
                    <span className="p-1.5 bg-slate-900 rounded border border-slate-800">:feature:simulator</span>
                  </div>
                </div>

                <div className="flex justify-center text-slate-600 font-bold">↓ depends only on contracts ↓</div>

                <div className="p-4 bg-slate-950 rounded-xl border border-emerald-900/50 text-center">
                  <div className="font-bold text-emerald-400 text-sm mb-1">:core MODULES (Canonical Contracts & Infrastructure)</div>
                  <div className="text-slate-400">:core:contracts • :core:model • :core:ui • :core:device • :core:ai • :core:database • :core:storage</div>
                </div>
              </div>
            </div>

            {/* Build Order */}
            <div className="bg-slate-900 p-6 rounded-2xl border border-slate-800 space-y-4">
              <h3 className="text-lg font-bold text-white flex items-center space-x-2">
                <CheckCircle2 className="w-5 h-5 text-emerald-400" />
                <span>13-Stage Architectural Build Order</span>
              </h3>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-sm">
                {[
                  { stage: '1', title: 'Android Project & Stack', exit: 'Base project launches' },
                  { stage: '2', title: 'Core Contracts', exit: 'Canonical types compile' },
                  { stage: '3', title: 'App Shell & Navigation', exit: 'Route transitions work' },
                  { stage: '4', title: 'Developer Simulator', exit: 'Deterministic fake world' },
                  { stage: '5', title: 'Device Onboarding', exit: 'Simulated Robo paired' },
                  { stage: '6', title: 'Home + Energy Center', exit: 'Dashboard consumes contracts' },
                  { stage: '7', title: 'Control + Safety Gate', exit: 'Movement commands gated' },
                  { stage: '8', title: 'Robo Talk (AI)', exit: 'Mock AiEngine works' },
                  { stage: '9', title: 'Camera + Environment', exit: 'CameraX preview renders' },
                  { stage: '10', title: 'Activity + Health + Alerts', exit: 'Ledger & inbox visible' },
                  { stage: '11', title: 'Analytics + Settings', exit: 'Preferences & yields persist' },
                  { stage: '12', title: 'Production Adapters', exit: 'Mocks swapped for ESP32 BLE' },
                  { stage: '13', title: 'Physical Validation', exit: 'Field testing on hardware' }
                ].map((s) => (
                  <div key={s.stage} className="p-3 bg-slate-950 rounded-lg border border-slate-800 flex items-start space-x-3">
                    <span className="w-6 h-6 rounded-full bg-amber-500/20 text-amber-400 text-xs flex items-center justify-center font-bold">
                      {s.stage}
                    </span>
                    <div>
                      <div className="font-semibold text-white">{s.title}</div>
                      <div className="text-xs text-slate-400">Exit: {s.exit}</div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}

        {/* TAB 2: TECH STACK */}
        {activeTab === 'techstack' && (
          <div className="max-w-5xl mx-auto space-y-6">
            <div className="border-b border-slate-800 pb-5">
              <h2 className="text-3xl font-extrabold text-white">Locked Android Technology Stack</h2>
              <p className="text-slate-400 mt-2">
                Authoritative specifications from Section 2 of the Solar Robo architecture. No unapproved third-party dependencies.
              </p>
            </div>

            <div className="bg-slate-900 rounded-2xl border border-slate-800 overflow-hidden">
              <table className="w-full text-left border-collapse text-sm">
                <thead>
                  <tr className="bg-slate-800/60 text-slate-300 font-semibold border-b border-slate-700">
                    <th className="p-4">Area</th>
                    <th className="p-4">Technology</th>
                    <th className="p-4">Architectural Role</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800">
                  {[
                    { area: 'Platform', tech: 'Android (API 26 - 35)', role: 'Native mobile operating system' },
                    { area: 'Language', tech: 'Kotlin 1.9+', role: 'Application source code, Coroutines' },
                    { area: 'UI Framework', tech: 'Jetpack Compose', role: 'Declarative screens, @Preview, Live Edit' },
                    { area: 'Architecture', tech: 'Clean Architecture + MVVM/UDF', role: 'Unidirectional state flow (UiState)' },
                    { area: 'DI', tech: 'Hilt (Dagger)', role: 'Compile-time dependency injection' },
                    { area: 'Async & Reactive', tech: 'Coroutines + Flow / StateFlow', role: 'Non-blocking I/O & reactive state' },
                    { area: 'Navigation', tech: 'Navigation Compose', role: 'Type-safe screen routing' },
                    { area: 'Persistence (DB)', tech: 'Room Database', role: 'Local SQLite for history & events' },
                    { area: 'Persistence (Key-Value)', tech: 'Jetpack DataStore', role: 'Preferences & device pairing tokens' },
                    { area: 'Networking', tech: 'OkHttp + Retrofit + WebSocket', role: 'REST telemetry & socket streams' },
                    { area: 'Device I/O', tech: 'Android Bluetooth LE (BLE)', role: 'ESP32 GATT scanning & characteristic I/O' },
                    { area: 'Camera', tech: 'CameraX', role: 'Real-time video preview & snapshot capture' },
                    { area: 'Background Jobs', tech: 'WorkManager', role: 'Periodic health audits & sync' },
                    { area: 'AI Inference', tech: 'Custom AiEngine Interface', role: 'Model abstraction (Gemini Flash / On-Device)' },
                    { area: 'Testing', tech: 'JUnit + MockK + Compose UI Test', role: 'Unit tests & headless UI verification' },
                    { area: 'Build System', tech: 'Gradle Kotlin DSL', role: 'Multi-module build configuration' }
                  ].map((row, idx) => (
                    <tr key={idx} className="hover:bg-slate-800/30 transition">
                      <td className="p-4 font-semibold text-amber-400">{row.area}</td>
                      <td className="p-4 font-mono text-slate-200">{row.tech}</td>
                      <td className="p-4 text-slate-400">{row.role}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* TAB 3: CONTRACTS */}
        {activeTab === 'contracts' && (
          <div className="max-w-5xl mx-auto space-y-6">
            <div className="border-b border-slate-800 pb-5">
              <h2 className="text-3xl font-extrabold text-white">Canonical Cross-Module Contracts</h2>
              <p className="text-slate-400 mt-2">
                All data crossing module boundaries MUST use these types from <code className="text-amber-400">:core:contracts</code>. No cross-feature dependencies.
              </p>
            </div>

            <div className="space-y-6">
              {[
                {
                  title: 'RoboSnapshot & Operating Modes',
                  file: 'core/contracts/RoboSnapshot.kt',
                  code: `data class RoboSnapshot(\n    val deviceId: String,\n    val name: String,\n    val connected: Boolean,\n    val mode: RoboMode,\n    val panelAngleDeg: Float,\n    val targetAngleDeg: Float,\n    val generationWatts: Float,\n    val batteryPercent: Float,\n    val timestamp: Long\n)\n\nenum class RoboMode {\n    NORMAL, OPTIMIZING, CONSERVING, PROTECTING, FAULT, SAFE\n}`
                },
                {
                  title: 'RoboCommand & Deterministic Execution Result',
                  file: 'core/contracts/RoboCommand.kt',
                  code: `sealed interface RoboCommand {\n    data class MoveToAngle(val angleDeg: Float) : RoboCommand\n    data object StopMotion : RoboCommand\n    data object SafePosition : RoboCommand\n}\n\ndata class CommandResult(\n    val accepted: Boolean,\n    val commandId: String,\n    val reason: String? = null,\n    val timestamp: Long = System.currentTimeMillis()\n)`
                },
                {
                  title: 'Deterministic Safety Gate Decisions & Events',
                  file: 'core/contracts/SafetyContracts.kt',
                  code: `sealed interface SafetyDecision {\n    data object Allow : SafetyDecision\n    data class Block(val reason: String) : SafetyDecision\n    data class Modify(val command: RoboCommand, val reason: String) : SafetyDecision\n}\n\nenum class SafetyLevel { NORMAL, CAUTION, PROTECTING, FAULT, EMERGENCY }\n\ndata class SafetyEvent(\n    val id: String,\n    val level: SafetyLevel,\n    val code: String,\n    val message: String,\n    val createdAt: Long,\n    val acknowledged: Boolean = false\n)`
                },
                {
                  title: 'Hardware & AI Core Interfaces',
                  file: 'core/device/RoboDevice.kt & core/ai/AiEngine.kt',
                  code: `interface RoboDevice {\n    suspend fun connect(): Result<Unit>\n    suspend fun disconnect()\n    suspend fun getSnapshot(): RoboSnapshot\n    suspend fun sendCommand(command: RoboCommand): CommandResult\n    fun observeSnapshot(): Flow<RoboSnapshot>\n}\n\ndata class AiRequest(val message: String, val context: Map<String, Any?>)\ndata class AiResponse(val text: String, val intent: String? = null, val confidence: Float? = null)\n\ninterface AiEngine {\n    suspend fun generate(request: AiRequest): AiResponse\n}`
                }
              ].map((item, idx) => (
                <div key={idx} className="bg-slate-900 rounded-xl border border-slate-800 overflow-hidden">
                  <div className="p-4 bg-slate-800/60 border-b border-slate-700 flex justify-between items-center">
                    <div>
                      <h4 className="font-semibold text-white">{item.title}</h4>
                      <span className="text-xs text-slate-400 font-mono">{item.file}</span>
                    </div>
                    <button
                      onClick={() => copyToClipboard(item.code, item.file)}
                      className="px-3 py-1.5 rounded-lg bg-slate-700 hover:bg-slate-600 text-xs font-medium text-slate-200 flex items-center space-x-1.5 transition"
                    >
                      {copiedCode === item.file ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                      <span>{copiedCode === item.file ? 'Copied' : 'Copy'}</span>
                    </button>
                  </div>
                  <pre className="p-4 text-xs font-mono text-emerald-300 bg-slate-950 overflow-x-auto">
                    <code>{item.code}</code>
                  </pre>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* TAB 4: MODULES EXPLORER */}
        {activeTab === 'modules' && (
          <div className="max-w-6xl mx-auto space-y-6">
            <div className="border-b border-slate-800 pb-5">
              <h2 className="text-3xl font-extrabold text-white">Module Specifications & Skeleton Code</h2>
              <p className="text-slate-400 mt-2">
                Select any module below to inspect its exact I/O matrix, file layout, constraints, and complete Kotlin skeleton code.
              </p>
            </div>

            {/* Horizontal Module Chips */}
            <div className="flex space-x-2 overflow-x-auto pb-2 scrollbar-thin">
              {MODULES.map((m) => {
                const Icon = m.icon;
                const isSelected = m.id === currentModule.id;
                return (
                  <button
                    key={m.id}
                    onClick={() => setSelectedModuleId(m.id)}
                    className={`flex items-center space-x-2 px-4 py-2.5 rounded-xl text-xs font-semibold whitespace-nowrap transition border ${
                      isSelected
                        ? 'bg-amber-500 text-slate-950 border-amber-400 shadow-lg shadow-amber-500/10'
                        : 'bg-slate-900 text-slate-300 border-slate-800 hover:bg-slate-800 hover:text-white'
                    }`}
                  >
                    <Icon className="w-4 h-4" />
                    <span>{m.name}</span>
                  </button>
                );
              })}
            </div>

            {/* Selected Module Detail */}
            <div className="bg-slate-900 rounded-2xl border border-slate-800 p-6 space-y-6">
              <div className="flex items-start justify-between">
                <div>
                  <h3 className="text-2xl font-bold text-white">{currentModule.title}</h3>
                  <p className="text-slate-300 mt-1">{currentModule.task}</p>
                </div>
                <span className="px-3 py-1 bg-amber-500/10 text-amber-400 border border-amber-500/20 rounded-full text-xs font-mono">
                  docs/modules/{currentModule.id}.md
                </span>
              </div>

              {/* Exact I/O Matrix */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="p-4 bg-slate-950 rounded-xl border border-slate-800 space-y-2">
                  <div className="text-xs font-bold uppercase tracking-wider text-emerald-400">Declared Inputs (IN)</div>
                  <div className="text-sm font-mono text-slate-200">{currentModule.inputs}</div>
                </div>

                <div className="p-4 bg-slate-950 rounded-xl border border-slate-800 space-y-2">
                  <div className="text-xs font-bold uppercase tracking-wider text-sky-400">Declared Outputs (OUT)</div>
                  <div className="text-sm font-mono text-slate-200">{currentModule.outputs}</div>
                </div>
              </div>

              {/* Strict Invariants */}
              <div className="p-4 bg-red-950/20 border border-red-900/40 rounded-xl space-y-2">
                <div className="text-xs font-bold uppercase tracking-wider text-red-400 flex items-center space-x-2">
                  <ShieldAlert className="w-4 h-4" />
                  <span>Must NOT Implement (Scope Boundaries)</span>
                </div>
                <ul className="text-sm text-slate-300 space-y-1 list-disc list-inside">
                  {currentModule.forbidden.map((f, i) => (
                    <li key={i}>{f}</li>
                  ))}
                </ul>
              </div>

              {/* Onboarding Live Interactive Runtime Verification */}
              {currentModule.id === 'onboarding' && (
                <div className="p-5 bg-slate-950 rounded-2xl border border-amber-500/30 space-y-4">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-2">
                      <div className="w-2.5 h-2.5 rounded-full bg-emerald-400 animate-ping" />
                      <h4 className="font-bold text-white text-base">Interactive Module Verification: Onboarding Flow</h4>
                    </div>
                    <span className="text-xs font-mono text-amber-400">Step: {onbStep}</span>
                  </div>

                  {onbStep === 'DISCOVERY' && (
                    <div className="space-y-3">
                      <p className="text-xs text-slate-400">
                        Simulate BLE peripheral scanning to discover nearby Solar Robo microcontrollers.
                      </p>
                      <button
                        onClick={() => {
                          setOnbScanning(true);
                          setTimeout(() => {
                            setOnbScanning(false);
                            setOnbDevices([
                              { id: 'ROBO-ESP32-01', name: 'Solar Robo Alpha', rssi: -62 },
                              { id: 'ROBO-ESP32-02', name: 'Solar Robo Garden Unit', rssi: -78 }
                            ]);
                          }, 600);
                        }}
                        disabled={onbScanning}
                        className="px-4 py-2 bg-amber-500 hover:bg-amber-400 text-slate-950 font-bold text-xs rounded-xl transition"
                      >
                        {onbScanning ? 'Scanning Bluetooth LE...' : 'Scan for Nearby Peripherals'}
                      </button>

                      {onbDevices.length > 0 && (
                        <div className="space-y-2 pt-2">
                          <div className="text-xs font-bold text-slate-300 uppercase tracking-wider">Discovered Peripherals (Tap to Pair):</div>
                          {onbDevices.map((d) => (
                            <div
                              key={d.id}
                              onClick={() => {
                                setOnbSelectedDevice(d);
                                setOnbStep('CONFIGURATION');
                              }}
                              className="p-3 bg-slate-900 hover:bg-slate-800 rounded-xl border border-slate-800 flex justify-between items-center cursor-pointer transition"
                            >
                              <div>
                                <div className="text-sm font-semibold text-white">{d.name}</div>
                                <div className="text-xs font-mono text-slate-400">ID: {d.id} • Signal: {d.rssi} dBm</div>
                              </div>
                              <span className="text-xs px-2.5 py-1 bg-amber-500/10 text-amber-400 rounded-lg border border-amber-500/20">Select</span>
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  )}

                  {onbStep === 'CONFIGURATION' && (
                    <div className="space-y-3">
                      <div className="text-sm font-semibold text-amber-400">
                        Configuring Device: {onbSelectedDevice?.name} ({onbSelectedDevice?.id})
                      </div>
                      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                        <div>
                          <label className="text-xs text-slate-400 block mb-1">Friendly Name</label>
                          <input
                            type="text"
                            value={onbName}
                            onChange={(e) => setOnbName(e.target.value)}
                            className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-1.5 text-xs text-white"
                          />
                        </div>
                        <div>
                          <label className="text-xs text-slate-400 block mb-1">Wi-Fi SSID</label>
                          <input
                            type="text"
                            value={onbSsid}
                            onChange={(e) => setOnbSsid(e.target.value)}
                            className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-1.5 text-xs text-white"
                          />
                        </div>
                        <div>
                          <label className="text-xs text-slate-400 block mb-1">Wi-Fi Password</label>
                          <input
                            type="password"
                            value={onbPass}
                            onChange={(e) => setOnbPass(e.target.value)}
                            className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-1.5 text-xs text-white"
                          />
                        </div>
                      </div>
                      <div className="flex space-x-2 pt-2">
                        <button
                          onClick={() => {
                            setOnbConnecting(true);
                            setTimeout(() => {
                              setOnbConnecting(false);
                              setOnbStep('COMPLETED');
                            }, 800);
                          }}
                          disabled={onbConnecting}
                          className="px-4 py-2 bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-bold text-xs rounded-xl transition"
                        >
                          {onbConnecting ? 'Verifying BLE Handshake...' : 'Verify Connection & Save'}
                        </button>
                        <button
                          onClick={() => setOnbStep('DISCOVERY')}
                          className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs rounded-xl transition"
                        >
                          Cancel
                        </button>
                      </div>
                    </div>
                  )}

                  {onbStep === 'COMPLETED' && (
                    <div className="p-4 bg-emerald-950/20 border border-emerald-900/40 rounded-xl space-y-2">
                      <div className="flex items-center space-x-2 text-emerald-400 font-bold text-sm">
                        <CheckCircle2 className="w-4 h-4" />
                        <span>Pairing Completed Successfully!</span>
                      </div>
                      <p className="text-xs text-slate-300">
                        Device <strong>{onbName}</strong> ({onbSelectedDevice?.id}) is configured. Emitted <code className="text-amber-300">DeviceAdded</code> event to Event Bus. Ready to transition to Command Center.
                      </p>
                      <button
                        onClick={() => {
                          setOnbStep('DISCOVERY');
                          setOnbDevices([]);
                          setOnbSelectedDevice(null);
                        }}
                        className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-xs text-slate-300 rounded-lg transition"
                      >
                        Reset Onboarding Simulator
                      </button>
                    </div>
                  )}
                </div>
              )}

              {/* Skeleton Code Files */}
              <div className="space-y-4">
                <h4 className="text-lg font-bold text-white flex items-center space-x-2">
                  <FileCode className="w-5 h-5 text-amber-400" />
                  <span>Module Files & Skeleton Implementations</span>
                </h4>

                <div className="space-y-4">
                  {currentModule.files.map((file, i) => (
                    <div key={i} className="bg-slate-950 rounded-xl border border-slate-800 overflow-hidden">
                      <div className="p-3 bg-slate-800/40 border-b border-slate-800 flex justify-between items-center">
                        <div>
                          <span className="font-mono text-sm font-semibold text-white">{file.name}</span>
                          <span className="ml-2 px-2 py-0.5 text-xs rounded bg-slate-800 text-slate-300">{file.type}</span>
                          <p className="text-xs text-slate-400 mt-0.5">{file.desc}</p>
                        </div>
                        <button
                          onClick={() => copyToClipboard(file.code, `${currentModule.id}-${file.name}`)}
                          className="px-3 py-1 rounded bg-slate-800 hover:bg-slate-700 text-xs font-medium text-slate-200 flex items-center space-x-1 transition"
                        >
                          {copiedCode === `${currentModule.id}-${file.name}` ? (
                            <Check className="w-3.5 h-3.5 text-emerald-400" />
                          ) : (
                            <Copy className="w-3.5 h-3.5" />
                          )}
                          <span>{copiedCode === `${currentModule.id}-${file.name}` ? 'Copied' : 'Copy'}</span>
                        </button>
                      </div>
                      <pre className="p-4 text-xs font-mono text-emerald-300 overflow-x-auto">
                        <code>{file.code}</code>
                      </pre>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>
        )}

        {/* TAB 5: AGENTS.MD & RULES */}
        {activeTab === 'agents' && (
          <div className="max-w-5xl mx-auto space-y-6">
            <div className="border-b border-slate-800 pb-5">
              <h2 className="text-3xl font-extrabold text-white">AGENTS.md & Coding Protocol</h2>
              <p className="text-slate-400 mt-2">
                Mandatory rules for AI agents (Antigravity, Claude, Codex, Android Studio AI) implementing Solar Robo modules.
              </p>
            </div>

            <div className="bg-slate-900 rounded-2xl border border-slate-800 p-6 space-y-5">
              <div className="flex justify-between items-center">
                <h3 className="text-lg font-bold text-white">Autonomous Agent Prompt Template</h3>
                <button
                  onClick={() =>
                    copyToClipboard(
                      `TASK: Implement ONLY <MODULE>.\nREAD FIRST: AGENTS.md, docs/SYSTEM_ARCHITECTURE.md, docs/modules/<MODULE>.md\nDO: create specified files, implement only listed functions, exact I/O, provide FakeRepository, add Compose @Previews, run unit tests.\nDO NOT: redesign architecture, add unrequested features, import another feature module, bypass safety gate.\nIF BLOCKED: STOP and report missing contract.`,
                      'agent-prompt'
                    )
                  }
                  className="px-3 py-1.5 rounded-lg bg-amber-500 text-slate-950 font-bold text-xs flex items-center space-x-1.5"
                >
                  {copiedCode === 'agent-prompt' ? <Check className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5" />}
                  <span>Copy Prompt</span>
                </button>
              </div>

              <div className="p-4 bg-slate-950 rounded-xl border border-slate-800 font-mono text-xs text-amber-300 leading-relaxed">
                TASK: Implement ONLY &lt;MODULE&gt;.<br />
                READ: AGENTS.md, SYSTEM_ARCHITECTURE.md, module spec, relevant contracts.<br />
                DO: create specified files; implement specified functions; exact I/O; mocks; previews; tests; build.<br />
                DO NOT: redesign architecture; add features; import another feature; bypass safety.<br />
                IF BLOCKED: STOP and report missing contract.<br />
                FINAL REPORT: Files / Functions / Inputs / Outputs / Tests / Scope additions.
              </div>

              <div className="space-y-3 pt-3">
                <h4 className="text-sm font-bold uppercase tracking-wider text-slate-400">Definition of Done Checklist</h4>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-sm">
                  {[
                    'Specified files exist and adhere to exact paths',
                    'Zero feature-to-feature module imports',
                    'All types sourced from :core:contracts',
                    'FakeRepository provides standalone preview state',
                    'All Compose components include @Preview',
                    'Physical commands pass deterministic safety policy',
                    'Unit tests pass with JUnit & test coroutine dispatcher',
                    'GitHub Actions CI passes without warnings'
                  ].map((chk, i) => (
                    <div key={i} className="p-3 bg-slate-950 rounded-lg border border-slate-800 flex items-center space-x-2">
                      <CheckCircle2 className="w-4 h-4 text-emerald-400 flex-shrink-0" />
                      <span className="text-slate-300 text-xs">{chk}</span>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>
        )}

        {/* TAB 6: INTERACTIVE SIMULATOR */}
        {activeTab === 'simulator' && (
          <div className="max-w-5xl mx-auto space-y-6">
            <div className="border-b border-slate-800 pb-5">
              <h2 className="text-3xl font-extrabold text-white">Interactive Solar Robo Simulator</h2>
              <p className="text-slate-400 mt-2">
                Simulate weather conditions and hardware scenarios in real-time. Notice how the deterministic Safety Gate intercepts high-wind or low-battery conditions without requiring real hardware.
              </p>
            </div>

            {/* Scenario Picker */}
            <div className="bg-slate-900 p-6 rounded-2xl border border-slate-800 space-y-4">
              <h3 className="font-bold text-white text-base">Select Test Scenario</h3>
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
                {[
                  { id: 'SUNNY_NORMAL', label: '☀️ Sunny (Nominal)' },
                  { id: 'HIGH_WIND', label: '💨 High Wind (>15 m/s)' },
                  { id: 'RAIN', label: '🌧️ Heavy Rain' },
                  { id: 'BATTERY_LOW', label: '🪫 Low Battery (<10%)' },
                  { id: 'MOTOR_JAM', label: '⚠️ Motor Stall Fault' }
                ].map((s) => (
                  <button
                    key={s.id}
                    onClick={() => handleScenarioChange(s.id)}
                    className={`p-3 rounded-xl text-xs font-semibold border transition text-left ${
                      simScenario === s.id
                        ? 'bg-amber-500 text-slate-950 border-amber-400 shadow-md font-bold'
                        : 'bg-slate-950 text-slate-300 border-slate-800 hover:border-slate-700'
                    }`}
                  >
                    {s.label}
                  </button>
                ))}
              </div>
            </div>

            {/* Live Telemetry Display */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div className="p-5 bg-slate-900 rounded-2xl border border-slate-800 space-y-3">
                <div className="text-xs uppercase text-slate-400 font-bold flex items-center space-x-1.5">
                  <Sun className="w-4 h-4 text-amber-400" />
                  <span>Solar Generation</span>
                </div>
                <div className="text-3xl font-extrabold text-white">{simWatts} W</div>
                <div className="text-xs text-slate-400">Current Tracker Angle: {simAngle}°</div>
              </div>

              <div className="p-5 bg-slate-900 rounded-2xl border border-slate-800 space-y-3">
                <div className="text-xs uppercase text-slate-400 font-bold flex items-center space-x-1.5">
                  <Battery className="w-4 h-4 text-emerald-400" />
                  <span>Battery Reserve</span>
                </div>
                <div className="text-3xl font-extrabold text-white">{simBattery}%</div>
                <div className="w-full bg-slate-950 rounded-full h-2 overflow-hidden border border-slate-800">
                  <div
                    className={`h-full ${simBattery < 15 ? 'bg-red-500' : 'bg-emerald-400'}`}
                    style={{ width: `${simBattery}%` }}
                  />
                </div>
              </div>

              <div className="p-5 bg-slate-900 rounded-2xl border border-slate-800 space-y-3">
                <div className="text-xs uppercase text-slate-400 font-bold flex items-center space-x-1.5">
                  <Wind className="w-4 h-4 text-sky-400" />
                  <span>Atmospheric Wind</span>
                </div>
                <div className="text-3xl font-extrabold text-white">{simWind} m/s</div>
                <div className="text-xs text-slate-400">Rain Sensor: {simRain ? 'DETECTED' : 'DRY'}</div>
              </div>
            </div>

            {/* Safety Gate Decision Banner */}
            <div className="p-6 bg-slate-900 rounded-2xl border border-slate-800 space-y-3">
              <div className="flex items-center space-x-2">
                <ShieldAlert className="w-5 h-5 text-amber-400" />
                <h4 className="font-bold text-white text-base">Deterministic Safety Gate Decision</h4>
              </div>
              <div className="p-4 bg-slate-950 rounded-xl border border-slate-800 font-mono text-sm">
                <div className="text-xs text-slate-400 mb-1">Incoming Command: MoveToAngle({simAngle}°)</div>
                <div className="font-bold text-amber-400">Gate Output: {simSafetyState}</div>
              </div>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}
