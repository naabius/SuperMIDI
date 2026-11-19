SuperMIDI {
	classvar <>path, <mappings, <bus, <midiOut, <>printBool, <>initiateBool;

	*initClass {
		mappings = Dictionary.new;
		bus = Dictionary.new;
		printBool = false;
		initiateBool = false;
	}

	//------------------------------------
	//----------public methods------------s
	//------------------------------------

	*init {arg midiOutPort = 0;
		MIDIClient.init;
		MIDIIn.connectAll;
		midiOut = MIDIOut(midiOutPort);
		initiateBool = true;

		"SuperMIDI: Initialized".postln;
	}

	*trace { arg bool = false;
		printBool = bool;
	}

	*map {arg type, target, cc, chan = 0, min = 0, max = 1, init, curve = \lin;
		var name, patternName = nil, isPattern = false;

		if(initiateBool == false){SuperMIDI.init;};

		SuperMIDI.prValidate(type, target, nil, cc, chan, min, max, init, curve, false);

		name = SuperMIDI.prMakeData(type, target, patternName, cc, chan, min, max, init, curve, isPattern);
		SuperMIDI.prMakeMap(name);
	}

	*learn {arg type, target, min = 0, max = 1, init, curve = \lin;
		var name, patternName = nil, isPattern = false;

		if(initiateBool == false){SuperMIDI.init;};

		SuperMIDI.prValidate(type, target, nil, nil, nil, min, max, init, curve, false);

		"SuperMIDI: move your parameter or press a toggle".postln;
		MIDIdef.cc(\temp, { arg val, cc, chan, src;
			MIDIdef(\temp).free;
			name = SuperMIDI.prMakeData(type, target, patternName, cc, chan, min, max, init, curve, isPattern);
			SuperMIDI.prMakeMap(name);
		});
	}

	*pmap {arg type, target, patternName, cc, chan = 0, min = 0, max = 1, init, curve = \lin;
		var name, isPattern = true;

		if(initiateBool == false){SuperMIDI.init;};

		SuperMIDI.prValidate(type, target, patternName, cc, chan, min, max, init, curve, true);

		name = SuperMIDI.prMakeData(type, target, patternName, cc, chan, min, max, init, curve, isPattern);
		SuperMIDI.prMakeMap(name);
	}

	*plearn {arg type, target, patternName, min = 0, max = 1, init = 0, curve = \lin;
		var name, isPattern = true;

		if(initiateBool == false){SuperMIDI.init;};

		SuperMIDI.prValidate(type, target, patternName, nil, nil, min, max, init, curve, true);

		"SuperMIDI: move your parameter or press a toggle".postln;
		MIDIdef.cc(\temp, { arg val, cc, chan, src;
			MIDIdef(\temp).free;
			name = SuperMIDI.prMakeData(type, target, patternName, cc, chan, min, max, init, curve, isPattern);
			SuperMIDI.prMakeMap(name);
		});
	}

	*mapList {arg keyword;
		var keys;

		if(mappings.isEmpty){
			"SuperMIDI: no mappings found".postln;
			^nil;
		};

		keys = mappings.keys.asArray.sort;

		if(keyword.notNil){
			keys = keys.select({ arg key;
				key.asString.containsi(keyword.asString);
			});
			if(keys.isEmpty){
				("SuperMIDI: keyword '" ++ keyword ++ "' not found").postln;
				^nil;
			};
		};

		"".postln;
		keys.do({ arg key;
			var val = mappings[key];
			var type = val[\type];
			var target = val[\target].asString;
			var patternName = val[\patternName].asString;
			var init = val[\init].asString;
			var curve = val[\curve].asString;
			var isPattern = val[\isPattern];

			if(isPattern && { [\toggle, \moment].includes(type) }){
				(key.asString ++ " | target: " ++ target ++ " | pattern: " ++ patternName).postln;
			}{
				if([\toggle, \moment].includes(type)){
					(key.asString ++ " | target: " ++ target).postln;
				}{
					(key.asString ++ " | target: " ++ target ++ " | curve: " ++ curve ++ " | init: " ++ init).postln;
				};
			};
		});
		"".postln;
	}

	*busList {arg keyword;
		var keys;

		if(bus.isEmpty){
			"SuperMIDI: no active bus found".postln;
			^nil;
		};

		keys = bus.keys.asArray.sort;

		if(keyword.notNil){
			keys = keys.select({ arg key;
				key.asString.containsi(keyword.asString);
			});
			if(keys.isEmpty){
				("SuperMIDI: keyword '" ++ keyword ++ "' not found").postln;
				^nil;
			};
		};

		"".postln;
		keys.do({ arg key;
			var b = bus[key];
			if(b.notNil){
				(key.asString ++ " | " ++ b.asString).postln;
			};
		});
		"".postln;
	}

	*remove {arg name;
		if(name.isNil){
			Error("SuperMIDI: no mapping name given to remove.").throw;
		};

		name = name.asSymbol;

		if(mappings[name].isNil){
			Error("SuperMIDI: mapping " ++ name ++ " not found.").throw;
		};

		MIDIdef(name).free;

		if(bus[name].notNil){
			bus[name].free;
			bus[name] = nil;
		};

		mappings.removeAt(name);
		("SuperMIDI: " ++ name ++ " removed").postln;
	}

	*removeAll {
		MIDIdef.freeAll;

		bus.keys.do({ arg key;
			if(bus[key].notNil){
				bus[key].free;
				bus[key] = nil;
			};
		});

		mappings = Dictionary.new;
		bus = Dictionary.new;
		("SuperMIDI: all mappings and buses are removed").postln;
	}

	*save { arg savePath;
		var file;

		if(savePath.notNil){
			path = savePath;
		};

		savePath = SuperMIDI.prResolvePath;
		path = SuperMIDI.prResolvePath;

		file = File(savePath, "w");
		if(file.isOpen){
			file.write(mappings.asJSON);
			file.close;
			("SuperMIDI: mappings saved to " ++ savePath).postln;
		}{
			Error("SuperMIDI: could not open file for writing at " ++ savePath).throw;
		};
	}

	*load { arg loadPath;
		var file, data;

		if(initiateBool == false){SuperMIDI.init;};

		if(loadPath.notNil){
			path = loadPath;
		};

		loadPath = SuperMIDI.prResolvePath;

		if(File.exists(loadPath).not){
			("SuperMIDI: no saved file found at " ++ loadPath).postln;
			^nil;
		};

		file = File(loadPath, "r");
		data = file.readAllString.parseJSON;
		file.close;

		if(data.isNil){
			Error("SuperMIDI: could not parse JSON from " ++ loadPath).throw;
		};

		data.keysValuesDo({ arg key, val;
			var name, target, patternName, isPattern;
			if(val.notNil){
				target      = if(val["target"].asString      == "nil"){ nil }{ val["target"].asSymbol };
				patternName = if(val["patternName"].asString  == "nil"){ nil }{ val["patternName"].asSymbol };
				isPattern   = val["isPattern"] == "true";

				name = SuperMIDI.prMakeData(
					val["type"].asSymbol,
					target,
					patternName,
					val["cc"].asInteger,
					val["chan"].asInteger,
					val["min"].asFloat,
					val["max"].asFloat,
					val["init"].asFloat,
					val["curve"].asSymbol,
					isPattern
				);
				SuperMIDI.prMakeMap(name);
			};
		});
		("SuperMIDI: mappings loaded from " ++ loadPath).postln;
	}

	//------------------------------------
	//----------private methods-----------
	//------------------------------------


	*prResolvePath {
		if(path.notNil){ ^path };

		if(thisProcess.nowExecutingPath.notNil && { thisProcess.nowExecutingPath.notEmpty }){
			var scdName = PathName(thisProcess.nowExecutingPath).fileNameWithoutExtension;
			^thisProcess.nowExecutingPath.dirname +/+ (scdName ++ "_SuperMIDI.json");
		};

		Error("SuperMIDI: no path set and .scd file is not saved. Use SuperMIDI.path = ... or save your .scd file first.").throw;
	}

	*prMakeID {arg type, target, patternName, cc, chan, isPattern;
		var parts = List.new;
		if(isPattern){ parts.add("p") };

		[type, target, patternName, cc, chan].do({ arg item;
			if(item.notNil && { item != "" } && { item != \nil }){
				parts.add(item.asString);
			};
		});

		^parts.join("_").asSymbol;
	}

	*prMakeData { arg type, target, patternName, cc, chan, min, max, init, curve, isPattern;
		var name;

		name = SuperMIDI.prMakeID(type, target, patternName, cc, chan, isPattern);

		mappings.put(name, (
			type: type.asSymbol,
			target: if(target.isNil){ nil }{ target.asSymbol },
			patternName: if(patternName.isNil){ nil }{ patternName.asSymbol },
			cc: cc.asInteger,
			chan: chan.asInteger,
			min: min.asFloat,
			max: max.asFloat,
			init: if(init.isNil){ init = min }{ init.asFloat },
			curve: if(curve.isNumber){ curve }{ curve.asSymbol },
			isPattern: isPattern,
		));

		SuperMIDI.prMakeBus(name, init);
		^name;
	}

	*prMakeBus { arg name, init;
		var data     = mappings[name];
		var target   = data[\target];
		var isPattern = data[\isPattern];
		var curve    = data[\curve];
		var needsBus = isPattern.not || { [\play, \stop, \pause].includes(target).not };

		if(curve == \exp && { init == 0 }){ init = 1e-3 };
		if(needsBus){
			if(bus[name].isNil){
				bus.put(name, Bus.control(Server.default, 1));
				bus[name].set(init);
				("SuperMIDI: bus created → SuperMIDI.bus[\\" ++ name ++ "]").postln;
			}{
				bus[name].set(init);
				("SuperMIDI: bus exists → SuperMIDI.bus[\\" ++ name ++ "], reset to " ++ init).postln;
			};
		};
	}

	*prScale {arg name, val;
		var data = mappings[name];
		var curve = data[\curve];
		var min = data[\min];
		var max = data[\max];

		^case
		{ curve == \lin }{ val.linlin(0, 127, min, max) }
		{ curve == \exp }{
			if(min <= 0){ min = 1e-3; };
			val.linexp(0, 127, min, max);
		}
		{ curve == \sin }{ val.lincurve(0, 127, min, max, \sin) }
		{ curve.isNumber }{ val.lincurve(0, 127, min, max, curve) };
	}

	*prPatternAction {arg name, val;
		var data = mappings[name];
		var type = data[\type];
		var target = data[\target];
		var patternName = data[\patternName];

		case
		{ target == \play }{
			if(val > 0){
				Pbindef(patternName).play;
				("SuperMIDI: " ++ patternName ++ " playing").postln;
			};
		}
		{ target == \stop }{
			if((type == \moment && { val > 0 }) || { type == \toggle && { val == 0 } }){
				Pbindef(patternName).stop;
				("SuperMIDI: " ++ patternName ++ " stopped").postln;
			};
		}
		{ target == \pause }{
			if((type == \moment && { val > 0 }) || { type == \toggle && { val == 0 } }){
				Pbindef(patternName).pause;
				("SuperMIDI: " ++ patternName ++ " paused").postln;
			};
		};
	}

	*prMakeMap {arg name;
		var data = mappings[name];
		var target = data[\target];
		var patternName = data[\patternName];
		var isPattern = data[\isPattern];
		var isAction = [\play, \stop, \pause].includes(target);

		MIDIdef.cc(name, { arg val, num, chan, src;
			var scaled = SuperMIDI.prScale(name, val);

			if(bus[name].notNil){
				bus[name].set(scaled);
			};

			if(isPattern && { isAction.not }){
				Pbindef(patternName, target, Pfunc({ bus[name].asMap }));
			};

			if(isPattern && { isAction }){
				SuperMIDI.prPatternAction(name, val);
			};

			if(printBool){ [name, val, scaled].postln };

		}, ccNum: data[\cc], chan: data[\chan]);

		midiOut.control(data[\chan], data[\cc], data[\init].linlin(data[\min], data[\max], 0, 127));
		SuperMIDI.mapList(name.asSymbol);
	}

	*prValidate {arg type, target, patternName, cc, chan, min, max, init, curve, isPattern;

		if([\control, \toggle, \moment].includes(type).not){
			Error("SuperMIDI: invalid type '" ++ type ++ "'. Must be \\control, \\toggle or \\moment").throw;
		};
		if(target.isNumber){
			Error("SuperMIDI: invalid target '" ++ target ++ "'. Must be a symbol like \\freq").throw;
		};
		if(isPattern && { patternName.isNil }){
			Error("SuperMIDI: patternName cannot be nil for pattern mappings").throw;
		};
		if(cc.notNil && { cc.isNumber.not }){
			Error("SuperMIDI: invalid cc '" ++ cc ++ "'. Must be an integer 0-127").throw;
		};
		if(cc.notNil && { (cc < 0) || (cc > 127) }){
			Error("SuperMIDI: cc " ++ cc ++ " out of range. Must be 0-127").throw;
		};
		if(chan.notNil && { chan.isNumber.not }){
			Error("SuperMIDI: invalid chan '" ++ chan ++ "'. Must be an integer 0-15").throw;
		};
		if(chan.notNil && { (chan < 0) || (chan > 15) }){
			Error("SuperMIDI: chan " ++ chan ++ " out of range. Must be 0-15").throw;
		};
		if(min.isNumber.not){
			Error("SuperMIDI: invalid min '" ++ min ++ "'. Must be a number").throw;
		};
		if(max.isNumber.not){
			Error("SuperMIDI: invalid max '" ++ max ++ "'. Must be a number").throw;
		};
		if(init.isNumber.not && {init.isNil.not}){
			Error("SuperMIDI: invalid init '" ++ init ++ "'. Must be a number").throw;
		};
		if(curve.isNumber.not && { [\lin, \exp, \sin].includes(curve).not }){
			Error("SuperMIDI: invalid curve '" ++ curve ++ "'. Must be \\lin, \\exp, \\sin or a number").throw;
		};
	}
}