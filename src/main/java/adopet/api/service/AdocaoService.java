package adopet.api.service;

import adopet.api.dto.*;
import adopet.api.exception.AdocaoException;
import adopet.api.model.Adocao;
import adopet.api.model.Pet;
import adopet.api.model.StatusAdocao;
import adopet.api.model.Tutor;
import adopet.api.repository.AdocaoRepository;
import adopet.api.repository.PetRepository;
import adopet.api.repository.TutorRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdocaoService {

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private TutorRepository tutorRepository;

    @Autowired
    private AdocaoRepository adocaoRepository;

    public List<AdocaoDTO> listarTodos(){

        return adocaoRepository.findAll().stream().map(AdocaoDTO::new).toList();
    }
    public AdocaoDTO listar(Long id){

        return adocaoRepository.findById(id).stream().findFirst().map(AdocaoDTO::new).orElse(null);
    }

    public void solicitar(SolicitacaoDeAdocaoDTO dto){
        Pet pet = petRepository.getReferenceById(dto.idPet());
        Tutor tutor = tutorRepository.getReferenceById(dto.idTutor());

        //Verificar se o Pet já foi adotado
        if(pet.getAdotado()){
            throw new AdocaoException("Solicitação negada: Pet já adotado.");
        }

        //Verificar se esta solicitação já está em andamento
        Boolean solicitacaoEmAndamento = adocaoRepository.existsByPetIdAndStatus(dto.idPet(), StatusAdocao.AGUARDANDO_AVALIACAO);
        if(solicitacaoEmAndamento){
            throw new AdocaoException("Solicitação negada: Este pet já está com uma adoção em andamento.");
        }

        //Verificar se o Tutor já possui 2 adoções aprovadas.
        Integer numeroDeAdocoesDoTutor = adocaoRepository.countByTutorIdAndStatus(dto.idTutor(), StatusAdocao.APROVADO);
        if(numeroDeAdocoesDoTutor >= 2){
            throw new AdocaoException("Solicitação negada: Tutor já realizou 2 adoções.");
        }

        adocaoRepository.save(new Adocao(tutor,pet, dto.motivo()));
    }

    public void aprovar(AprovarAdocaoDTO dto){
        Adocao adocao = adocaoRepository.getReferenceById(dto.idAdocao());
        adocao.marcarComoAprovada();
        adocao.getPet().marcarComoAdotado();
    }

    public void reprovar(ReprovarAdocaoDTO dto){
        Adocao adocao = adocaoRepository.getReferenceById(dto.idAdocao());
        adocao.marcarComoReprovada(dto.justificativa());
    }
}
