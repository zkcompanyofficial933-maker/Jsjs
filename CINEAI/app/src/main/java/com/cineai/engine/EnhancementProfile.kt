package com.cineai.engine

enum class Scene { PORTRAIT, LANDSCAPE, INDOOR, OUTDOOR, NIGHT, LOW_LIGHT, SUNSET, FOOD, ARCHITECTURE, SPORTS, STREET, VEHICLE, GROUP, NATURE, UNKNOWN }
enum class Preset(val label:String){ AUTO("AUTO AI"), DSLR("DSLR Natural"), CINEMATIC("Cinematic"), FILM("Film"), CLEAN("Clean Pro"), WARM("Warm Cinema"), COOL("Cool Cinema"), NIGHT("Night Cinema"), PORTRAIT("Portrait Pro") }

data class EnhancementProfile(
    val exposure:Float=0f,val brightness:Float=0f,val contrast:Float=0.08f,val highlights:Float=-0.12f,val shadows:Float=0.10f,
    val whites:Float=-0.04f,val blacks:Float=0.02f,val temperature:Float=0f,val tint:Float=0f,val saturation:Float=0.02f,
    val vibrance:Float=0.08f,val sharpness:Float=0.15f,val texture:Float=0.08f,val noiseReduction:Float=0.10f,
    val vignette:Float=0f,val grain:Float=0f,val cinematicIntensity:Float=0.35f
){
    fun json()=listOf(exposure,brightness,contrast,highlights,shadows,whites,blacks,temperature,tint,saturation,vibrance,sharpness,texture,noiseReduction,vignette,grain,cinematicIntensity).joinToString(",")
    fun copyFrom(sliders:List<Float>) = if(sliders.size>=17) copy(exposure=sliders[0],brightness=sliders[1],contrast=sliders[2],highlights=sliders[3],shadows=sliders[4],whites=sliders[5],blacks=sliders[6],temperature=sliders[7],tint=sliders[8],saturation=sliders[9],vibrance=sliders[10],sharpness=sliders[11],texture=sliders[12],noiseReduction=sliders[13],vignette=sliders[14],grain=sliders[15],cinematicIntensity=sliders[16]) else this
    companion object {
        fun fromJson(value:String):EnhancementProfile { val a=value.split(",").mapNotNull{it.toFloatOrNull()}; return if(a.size>=17) EnhancementProfile(a[0],a[1],a[2],a[3],a[4],a[5],a[6],a[7],a[8],a[9],a[10],a[11],a[12],a[13],a[14],a[15],a[16]) else EnhancementProfile() }
        fun fromPreset(p:Preset, scene:Scene=Scene.UNKNOWN):EnhancementProfile {
            val base=when(p){
                Preset.DSLR->EnhancementProfile(contrast=.06f,highlights=-.16f,shadows=.09f,saturation=.0f,vibrance=.06f,sharpness=.10f,texture=.06f,noiseReduction=.10f,cinematicIntensity=.25f)
                Preset.CINEMATIC->EnhancementProfile(contrast=.14f,highlights=-.20f,shadows=.08f,temperature=.01f,saturation=-.02f,vibrance=.05f,sharpness=.08f,texture=.10f,cinematicIntensity=.65f)
                Preset.FILM->EnhancementProfile(contrast=.08f,highlights=-.14f,shadows=.12f,saturation=-.04f,vibrance=.08f,sharpness=.05f,texture=.04f,noiseReduction=.08f,grain=.05f,cinematicIntensity=.75f)
                Preset.CLEAN->EnhancementProfile(contrast=.10f,highlights=-.10f,shadows=.05f,vibrance=.10f,sharpness=.18f,texture=.12f,noiseReduction=.06f)
                Preset.WARM->EnhancementProfile(contrast=.10f,highlights=-.15f,shadows=.08f,temperature=.045f,saturation=.01f,vibrance=.07f,cinematicIntensity=.55f)
                Preset.COOL->EnhancementProfile(contrast=.12f,highlights=-.17f,shadows=.08f,temperature=-.035f,saturation=-.01f,vibrance=.06f,cinematicIntensity=.55f)
                Preset.NIGHT->EnhancementProfile(exposure=.05f,contrast=.04f,highlights=-.24f,shadows=.04f,saturation=-.03f,vibrance=.03f,sharpness=.03f,noiseReduction=.30f,cinematicIntensity=.30f)
                Preset.PORTRAIT->EnhancementProfile(contrast=.04f,highlights=-.10f,shadows=.10f,saturation=-.01f,vibrance=.04f,sharpness=.04f,texture=.02f,noiseReduction=.12f,cinematicIntensity=.25f)
                else->EnhancementProfile()
            }
            return when(scene){ Scene.NIGHT,Scene.LOW_LIGHT->base.copy(noiseReduction=maxOf(base.noiseReduction,.24f),sharpness=minOf(base.sharpness,.08f)); Scene.PORTRAIT,Scene.GROUP->base.copy(sharpness=minOf(base.sharpness,.08f),texture=minOf(base.texture,.05f)); Scene.LANDSCAPE,Scene.NATURE->base.copy(contrast=base.contrast+.03f,vibrance=base.vibrance+.04f,texture=base.texture+.03f); else->base }
        }
    }
}
